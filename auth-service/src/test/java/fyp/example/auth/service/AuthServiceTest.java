package fyp.example.auth.service;

import fyp.example.auth.entity.User;
import fyp.example.auth.repository.UserRepository;
import fyp.example.dto.auth.AuthResponseDto;
import fyp.example.dto.auth.LoginRequestDto;
import fyp.example.dto.auth.StudentRegisterDto;
import fyp.example.dto.auth.TeacherRegisterDto;
import fyp.example.dto.enums.AuthProvider;
import fyp.example.dto.enums.UserRole;
import fyp.example.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherKeyService teacherKeyService;

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Student registration: role is strictly ROLE_STUDENT on server")
    void shouldRegisterStudentWithRoleStudent() {
        StudentRegisterDto dto = StudentRegisterDto.builder()
                .fullName("John Doe")
                .email("student@university.edu")
                .password("StrongPassword123")
                .build();

        when(userRepository.existsByEmailIgnoreCase(dto.getEmail())).thenReturn(false);
        when(jwtTokenProvider.generateToken(any(UUID.class), eq(dto.getEmail()), eq(dto.getFullName()), eq(UserRole.ROLE_STUDENT)))
                .thenReturn("mock.jwt.token");

        AuthResponseDto response = authService.registerStudent(dto);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(UserRole.ROLE_STUDENT);
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_STUDENT);
        assertThat(passwordEncoder.matches("StrongPassword123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Teacher registration: verifies key and grants ROLE_TEACHER")
    void shouldRegisterTeacherWithRoleTeacherAfterKeyVerification() {
        TeacherRegisterDto dto = TeacherRegisterDto.builder()
                .fullName("Prof. Alan Turing")
                .email("turing@university.edu")
                .password("StrongPassword123")
                .secretKey("TC-ValidSecretKey")
                .build();

        when(userRepository.existsByEmailIgnoreCase(dto.getEmail())).thenReturn(false);
        doNothing().when(teacherKeyService).verifyTeacherKey(dto.getEmail(), dto.getSecretKey(), "127.0.0.1");
        when(jwtTokenProvider.generateToken(any(UUID.class), eq(dto.getEmail()), eq(dto.getFullName()), eq(UserRole.ROLE_TEACHER)))
                .thenReturn("mock.teacher.jwt.token");

        AuthResponseDto response = authService.registerTeacher(dto, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(UserRole.ROLE_TEACHER);
        assertThat(response.getToken()).isEqualTo("mock.teacher.jwt.token");

        verify(teacherKeyService).verifyTeacherKey(dto.getEmail(), dto.getSecretKey(), "127.0.0.1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_TEACHER);
    }

    @Test
    @DisplayName("Local login: succeeds with valid email and password")
    void shouldLoginSuccessfully() {
        String email = "student@university.edu";
        String rawPassword = "StrongPassword123";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .email(email)
                .passwordHash(hashedPassword)
                .role(UserRole.ROLE_STUDENT)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole()))
                .thenReturn("mock.login.jwt.token");

        LoginRequestDto request = LoginRequestDto.builder()
                .email(email)
                .password(rawPassword)
                .build();

        AuthResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.login.jwt.token");
        assertThat(response.getRole()).isEqualTo(UserRole.ROLE_STUDENT);
    }

    @Test
    @DisplayName("Local login: fails with 401 when password does not match")
    void shouldFailLoginWhenPasswordWrong() {
        String email = "student@university.edu";
        String hashedPassword = passwordEncoder.encode("CorrectPassword");

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .email(email)
                .passwordHash(hashedPassword)
                .role(UserRole.ROLE_STUDENT)
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        LoginRequestDto request = LoginRequestDto.builder()
                .email(email)
                .password("WrongPassword")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}

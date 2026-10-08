package fyp.example.auth.service;

import fyp.example.auth.entity.User;
import fyp.example.auth.repository.UserRepository;
import fyp.example.dto.auth.AuthResponseDto;
import fyp.example.dto.auth.GoogleAuthDto;
import fyp.example.dto.auth.LoginRequestDto;
import fyp.example.dto.auth.StudentRegisterDto;
import fyp.example.dto.auth.TeacherRegisterDto;
import fyp.example.dto.auth.UserDto;
import fyp.example.dto.enums.AuthProvider;
import fyp.example.dto.enums.UserRole;
import fyp.example.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TeacherKeyService teacherKeyService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password";

    @Transactional
    public AuthResponseDto registerStudent(StudentRegisterDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        validatePasswordStrength(dto.getPassword());

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(dto.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(UserRole.ROLE_STUDENT) // Server-enforced role
                .authProvider(AuthProvider.LOCAL)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);
        log.info("Student registered successfully with ID: {} and email: {}", user.getId(), user.getEmail());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponseDto registerTeacher(TeacherRegisterDto dto, String ipAddress) {
        String email = dto.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        validatePasswordStrength(dto.getPassword());

        // Verify teacher secret key with rate limiting & lockout
        teacherKeyService.verifyTeacherKey(email, dto.getSecretKey(), ipAddress);

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName(dto.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(UserRole.ROLE_TEACHER) // Granted only after key verification
                .authProvider(AuthProvider.LOCAL)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);
        log.info("Teacher registered successfully with ID: {} and email: {}", user.getId(), user.getEmail());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This account was registered using Google OAuth. Please login with Google.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public AuthResponseDto googleAuth(GoogleAuthDto dto, String ipAddress) {
        GoogleTokenVerifierService.GoogleUserInfo googleInfo = googleTokenVerifierService.verifyIdToken(dto.getIdToken());
        String email = googleInfo.email().trim().toLowerCase();

        return userRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> {
                    // Conflict policy: LOCAL account cannot login via Google without migration
                    if (existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "An account with this email already exists using password authentication. Please login with your password.");
                    }

                    String token = jwtTokenProvider.generateToken(existingUser.getId(), existingUser.getEmail(), existingUser.getFullName(), existingUser.getRole());
                    return AuthResponseDto.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .userId(existingUser.getId())
                            .fullName(existingUser.getFullName())
                            .email(existingUser.getEmail())
                            .role(existingUser.getRole())
                            .build();
                })
                .orElseGet(() -> {
                    // New user registering via Google
                    UserRole assignedRole = UserRole.ROLE_STUDENT;

                    if (dto.getTeacherSecretKey() != null && !dto.getTeacherSecretKey().isBlank()) {
                        teacherKeyService.verifyTeacherKey(email, dto.getTeacherSecretKey(), ipAddress);
                        assignedRole = UserRole.ROLE_TEACHER;
                    }

                    User newUser = User.builder()
                            .id(UUID.randomUUID())
                            .fullName(googleInfo.fullName())
                            .email(email)
                            .passwordHash("") // No password for OAuth users
                            .role(assignedRole)
                            .authProvider(AuthProvider.GOOGLE)
                            .createdAt(Instant.now())
                            .build();

                    userRepository.save(newUser);
                    log.info("New Google user registered with ID: {}, role: {}", newUser.getId(), newUser.getRole());

                    String token = jwtTokenProvider.generateToken(newUser.getId(), newUser.getEmail(), newUser.getFullName(), newUser.getRole());

                    return AuthResponseDto.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .userId(newUser.getId())
                            .fullName(newUser.getFullName())
                            .email(newUser.getEmail())
                            .role(newUser.getRole())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public UserDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }
}

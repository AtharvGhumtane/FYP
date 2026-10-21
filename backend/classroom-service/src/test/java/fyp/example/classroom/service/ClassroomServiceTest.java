package fyp.example.classroom.service;

import fyp.example.classroom.entity.ClassEntity;
import fyp.example.classroom.entity.Enrollment;
import fyp.example.classroom.repository.ClassRepository;
import fyp.example.classroom.repository.EnrollmentRepository;
import fyp.example.dto.classroom.ClassDto;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import fyp.example.dto.classroom.CreateClassDto;
import fyp.example.dto.classroom.EnrollmentDecisionDto;
import fyp.example.dto.classroom.EnrollmentDto;
import fyp.example.dto.enums.EnrollmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private NotificationDispatcherService notificationDispatcherService;

    @InjectMocks
    private ClassroomService classroomService;

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID OTHER_TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();

    @Test
    @DisplayName("Teacher creates class successfully")
    void shouldCreateClassSuccessfully() {
        CreateClassDto dto = CreateClassDto.builder()
                .name("Advanced Algorithms")
                .subject("Computer Science")
                .description("Data structures and complexity")
                .build();

        ClassDto result = classroomService.createClass(dto, TEACHER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Advanced Algorithms");
        assertThat(result.getSubject()).isEqualTo("Computer Science");
        assertThat(result.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(result.getClassCode()).isNotBlank();

        ArgumentCaptor<ClassEntity> captor = ArgumentCaptor.forClass(ClassEntity.class);
        verify(classRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Advanced Algorithms");
    }

    @Test
    @DisplayName("Student requests to join class: creates PENDING enrollment and notifies teacher")
    void shouldRequestJoinClass() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.findByClassEntityIdAndStudentId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.empty());

        EnrollmentDto result = classroomService.requestJoinClass(CLASS_ID, STUDENT_ID, "Alice Student", "alice@university.edu");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(notificationDispatcherService).notifyTeacherJoinRequest(eq(TEACHER_ID), eq("Alice Student"), eq("Database Systems"), eq(CLASS_ID));
    }

    @Test
    @DisplayName("Duplicate join request throws 409 Conflict")
    void shouldRejectDuplicateJoinRequest() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        Enrollment existingPending = Enrollment.builder()
                .id(UUID.randomUUID())
                .classEntity(classEntity)
                .studentId(STUDENT_ID)
                .status(EnrollmentStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.findByClassEntityIdAndStudentId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(existingPending));

        assertThatThrownBy(() -> classroomService.requestJoinClass(CLASS_ID, STUDENT_ID, "Alice Student", "alice@university.edu"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Owner teacher approves pending join request and notifies student")
    void shouldApproveJoinRequest() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        Enrollment pendingEnrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .classEntity(classEntity)
                .studentId(STUDENT_ID)
                .status(EnrollmentStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.findByClassEntityIdAndStudentId(CLASS_ID, STUDENT_ID)).thenReturn(Optional.of(pendingEnrollment));

        EnrollmentDecisionDto decision = EnrollmentDecisionDto.builder()
                .status(EnrollmentStatus.ACCEPTED)
                .build();

        EnrollmentDto result = classroomService.decideEnrollment(CLASS_ID, STUDENT_ID, decision, TEACHER_ID);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACCEPTED);
        assertThat(pendingEnrollment.getRespondedAt()).isNotNull();

        verify(notificationDispatcherService).notifyStudentJoinDecision(eq(STUDENT_ID), eq("Database Systems"), eq(true), eq(CLASS_ID));
    }

    @Test
    @DisplayName("Non-owner teacher cannot decide enrollment (403 Forbidden)")
    void shouldPreventNonOwnerTeacherFromDecidingEnrollment() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));

        EnrollmentDecisionDto decision = EnrollmentDecisionDto.builder()
                .status(EnrollmentStatus.ACCEPTED)
                .build();

        assertThatThrownBy(() -> classroomService.decideEnrollment(CLASS_ID, STUDENT_ID, decision, OTHER_TEACHER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Internal verification: verifies student enrollment correctly")
    void shouldVerifyStudentEnrollmentInternally() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));
        when(enrollmentRepository.existsByClassEntityIdAndStudentIdAndStatus(CLASS_ID, STUDENT_ID, EnrollmentStatus.ACCEPTED)).thenReturn(true);

        ClassVerificationResponseDto response = classroomService.verifyStudentEnrollment(CLASS_ID, STUDENT_ID);

        assertThat(response.isAuthorized()).isTrue();
        assertThat(response.getClassName()).isEqualTo("Database Systems");
    }

    @Test
    @DisplayName("Internal verification: verifies teacher ownership correctly")
    void shouldVerifyTeacherOwnershipInternally() {
        ClassEntity classEntity = ClassEntity.builder()
                .id(CLASS_ID)
                .name("Database Systems")
                .subject("CS")
                .classCode("DB101")
                .teacherId(TEACHER_ID)
                .createdAt(Instant.now())
                .build();

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(classEntity));

        ClassVerificationResponseDto ownerResponse = classroomService.verifyTeacherOwnership(CLASS_ID, TEACHER_ID);
        assertThat(ownerResponse.isAuthorized()).isTrue();

        ClassVerificationResponseDto nonOwnerResponse = classroomService.verifyTeacherOwnership(CLASS_ID, OTHER_TEACHER_ID);
        assertThat(nonOwnerResponse.isAuthorized()).isFalse();
    }
}

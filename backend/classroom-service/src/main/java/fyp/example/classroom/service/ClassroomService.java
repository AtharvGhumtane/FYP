package fyp.example.classroom.service;

import fyp.example.classroom.entity.ClassEntity;
import fyp.example.classroom.entity.Enrollment;
import fyp.example.classroom.repository.ClassRepository;
import fyp.example.classroom.repository.EnrollmentRepository;
import fyp.example.dto.classroom.*;
import fyp.example.dto.enums.EnrollmentStatus;
import fyp.example.dto.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomService {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationDispatcherService notificationDispatcherService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ClassDto createClass(CreateClassDto dto, UUID teacherId) {
        String classCode = generateClassCode();

        ClassEntity classEntity = ClassEntity.builder()
                .id(UUID.randomUUID())
                .name(dto.getName().trim())
                .subject(dto.getSubject().trim())
                .classCode(classCode)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .teacherId(teacherId)
                .createdAt(Instant.now())
                .build();

        classRepository.save(classEntity);
        log.info("Teacher {} created class '{}' ({}) with ID: {}", teacherId, classEntity.getName(), classCode, classEntity.getId());

        return mapToClassDto(classEntity);
    }

    @Transactional(readOnly = true)
    public List<ClassDto> getClassesForUser(UUID userId, UserRole role) {
        if (role == UserRole.ROLE_TEACHER) {
            return classRepository.findByTeacherIdOrderByCreatedAtDesc(userId).stream()
                    .map(this::mapToClassDto)
                    .collect(Collectors.toList());
        } else if (role == UserRole.ROLE_STUDENT) {
            return enrollmentRepository.findByStudentIdAndStatus(userId, EnrollmentStatus.ACCEPTED).stream()
                    .map(Enrollment::getClassEntity)
                    .map(this::mapToClassDto)
                    .collect(Collectors.toList());
        } else {
            // ROLE_ADMIN
            return classRepository.findAll().stream()
                    .map(this::mapToClassDto)
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public ClassDto getClassById(UUID classId, UUID userId, UserRole role) {
        ClassEntity classEntity = findClassOrThrow(classId);

        // Access check
        if (role == UserRole.ROLE_TEACHER && !classEntity.getTeacherId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this class");
        } else if (role == UserRole.ROLE_STUDENT) {
            boolean isEnrolled = enrollmentRepository.existsByClassEntityIdAndStudentIdAndStatus(classId, userId, EnrollmentStatus.ACCEPTED);
            if (!isEnrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not an enrolled student in this class");
            }
        }

        return mapToClassDto(classEntity);
    }

    @Transactional
    public EnrollmentDto requestJoinClass(UUID classId, UUID studentId, String studentName, String studentEmail) {
        ClassEntity classEntity = findClassOrThrow(classId);

        // Check existing enrollment
        enrollmentRepository.findByClassEntityIdAndStudentId(classId, studentId).ifPresent(existing -> {
            if (existing.getStatus() == EnrollmentStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already an accepted student in this class");
            } else if (existing.getStatus() == EnrollmentStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Your join request is already pending approval");
            }
        });

        // Upsert or create new enrollment
        Enrollment enrollment = enrollmentRepository.findByClassEntityIdAndStudentId(classId, studentId)
                .orElseGet(() -> Enrollment.builder()
                        .id(UUID.randomUUID())
                        .classEntity(classEntity)
                        .studentId(studentId)
                        .build());

        enrollment.setStudentName(studentName);
        enrollment.setStudentEmail(studentEmail);
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setRequestedAt(Instant.now());
        enrollment.setRespondedAt(null);

        enrollmentRepository.save(enrollment);
        log.info("Student {} requested to join class {} ({})", studentId, classId, classEntity.getName());

        notificationDispatcherService.notifyTeacherJoinRequest(classEntity.getTeacherId(), studentName, classEntity.getName(), classId);

        return mapToEnrollmentDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getPendingRequests(UUID classId, UUID teacherId) {
        ClassEntity classEntity = findClassOrThrow(classId);
        validateClassOwnership(classEntity, teacherId);

        return enrollmentRepository.findByClassEntityIdAndStatus(classId, EnrollmentStatus.PENDING).stream()
                .map(this::mapToEnrollmentDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnrollmentDto decideEnrollment(UUID classId, UUID studentId, EnrollmentDecisionDto decisionDto, UUID teacherId) {
        ClassEntity classEntity = findClassOrThrow(classId);
        validateClassOwnership(classEntity, teacherId);

        Enrollment enrollment = enrollmentRepository.findByClassEntityIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No join request found for this student"));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This enrollment request has already been decided: " + enrollment.getStatus());
        }

        EnrollmentStatus newStatus = decisionDto.getStatus();
        if (newStatus != EnrollmentStatus.ACCEPTED && newStatus != EnrollmentStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision status must be either ACCEPTED or REJECTED");
        }

        enrollment.setStatus(newStatus);
        enrollment.setRespondedAt(Instant.now());
        enrollmentRepository.save(enrollment);

        log.info("Teacher {} decided enrollment for student {} in class {}: {}", teacherId, studentId, classId, newStatus);

        notificationDispatcherService.notifyStudentJoinDecision(studentId, classEntity.getName(), newStatus == EnrollmentStatus.ACCEPTED, classId);

        return mapToEnrollmentDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<RosterStudentDto> getRoster(UUID classId, UUID userId, UserRole role) {
        ClassEntity classEntity = findClassOrThrow(classId);

        // Check permissions
        if (role == UserRole.ROLE_TEACHER && !classEntity.getTeacherId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this class roster");
        } else if (role == UserRole.ROLE_STUDENT) {
            boolean isEnrolled = enrollmentRepository.existsByClassEntityIdAndStudentIdAndStatus(classId, userId, EnrollmentStatus.ACCEPTED);
            if (!isEnrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not an enrolled student in this class");
            }
        }

        return enrollmentRepository.findByClassEntityIdAndStatus(classId, EnrollmentStatus.ACCEPTED).stream()
                .map(e -> RosterStudentDto.builder()
                        .studentId(e.getStudentId())
                        .fullName(e.getStudentName() != null ? e.getStudentName() : "Student")
                        .email(e.getStudentEmail() != null ? e.getStudentEmail() : "")
                        .build())
                .collect(Collectors.toList());
    }

    // ==========================================
    // Cross-Service Internal Verification APIs
    // ==========================================

    @Transactional(readOnly = true)
    public ClassVerificationResponseDto verifyStudentEnrollment(UUID classId, UUID studentId) {
        ClassEntity classEntity = classRepository.findById(classId).orElse(null);

        if (classEntity == null) {
            return ClassVerificationResponseDto.builder()
                    .classId(classId)
                    .isAuthorized(false)
                    .build();
        }

        boolean isAccepted = enrollmentRepository.existsByClassEntityIdAndStudentIdAndStatus(classId, studentId, EnrollmentStatus.ACCEPTED);

        return ClassVerificationResponseDto.builder()
                .classId(classId)
                .className(classEntity.getName())
                .teacherId(classEntity.getTeacherId())
                .isAuthorized(isAccepted)
                .build();
    }

    @Transactional(readOnly = true)
    public ClassVerificationResponseDto verifyTeacherOwnership(UUID classId, UUID teacherId) {
        ClassEntity classEntity = classRepository.findById(classId).orElse(null);

        if (classEntity == null) {
            return ClassVerificationResponseDto.builder()
                    .classId(classId)
                    .isAuthorized(false)
                    .build();
        }

        boolean isOwner = classEntity.getTeacherId().equals(teacherId);

        return ClassVerificationResponseDto.builder()
                .classId(classId)
                .className(classEntity.getName())
                .teacherId(classEntity.getTeacherId())
                .isAuthorized(isOwner)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UUID> getInternalRosterStudentIds(UUID classId) {
        findClassOrThrow(classId);
        return enrollmentRepository.findStudentIdsByClassIdAndStatus(classId, EnrollmentStatus.ACCEPTED);
    }

    private ClassEntity findClassOrThrow(UUID classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found with ID: " + classId));
    }

    private void validateClassOwnership(ClassEntity classEntity, UUID teacherId) {
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this class");
        }
    }

    private ClassDto mapToClassDto(ClassEntity entity) {
        return ClassDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .subject(entity.getSubject())
                .classCode(entity.getClassCode())
                .description(entity.getDescription())
                .teacherId(entity.getTeacherId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private EnrollmentDto mapToEnrollmentDto(Enrollment entity) {
        return EnrollmentDto.builder()
                .id(entity.getId())
                .classId(entity.getClassEntity().getId())
                .className(entity.getClassEntity().getName())
                .studentId(entity.getStudentId())
                .studentName(entity.getStudentName())
                .studentEmail(entity.getStudentEmail())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }

    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

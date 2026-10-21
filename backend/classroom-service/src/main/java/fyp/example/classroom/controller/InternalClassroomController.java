package fyp.example.classroom.controller;

import fyp.example.classroom.service.ClassroomService;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/classes")
@RequiredArgsConstructor
@Hidden
public class InternalClassroomController {

    private final ClassroomService classroomService;

    @GetMapping("/{classId}/verify-student/{studentId}")
    public ResponseEntity<ClassVerificationResponseDto> verifyStudentEnrollment(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(classroomService.verifyStudentEnrollment(classId, studentId));
    }

    @GetMapping("/{classId}/verify-teacher/{teacherId}")
    public ResponseEntity<ClassVerificationResponseDto> verifyTeacherOwnership(
            @PathVariable UUID classId,
            @PathVariable UUID teacherId) {
        return ResponseEntity.ok(classroomService.verifyTeacherOwnership(classId, teacherId));
    }

    @GetMapping("/{classId}/roster")
    public ResponseEntity<List<UUID>> getInternalRoster(@PathVariable UUID classId) {
        return ResponseEntity.ok(classroomService.getInternalRosterStudentIds(classId));
    }
}

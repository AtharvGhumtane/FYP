package fyp.example.classroom.controller;

import fyp.example.classroom.service.ClassroomService;
import fyp.example.dto.classroom.*;
import fyp.example.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classroom Management", description = "Classes creation, student join requests, and roster management")
@SecurityRequirement(name = "bearerAuth")
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a new classroom (Teacher only)")
    public ResponseEntity<ClassDto> createClass(@Valid @RequestBody CreateClassDto dto) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(classroomService.createClass(dto, teacherId));
    }

    @GetMapping
    @Operation(summary = "List classes relevant to the authenticated user")
    public ResponseEntity<List<ClassDto>> getClasses() {
        return ResponseEntity.ok(classroomService.getClassesForUser(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get class details (Teacher owner or enrolled student)")
    public ResponseEntity<ClassDto> getClassById(@PathVariable UUID id) {
        return ResponseEntity.ok(classroomService.getClassById(
                id,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole()
        ));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Request to join a class (Student only, creates PENDING enrollment)")
    public ResponseEntity<EnrollmentDto> requestJoinClass(@PathVariable UUID id) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        String studentName = SecurityUtils.getCurrentUser()
                .map(u -> u.getFullName())
                .orElse("Student");
        String studentEmail = SecurityUtils.getCurrentUser()
                .map(u -> u.getEmail())
                .orElse("");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classroomService.requestJoinClass(id, studentId, studentName, studentEmail));
    }

    @GetMapping("/{id}/requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "List pending join requests for a class (Teacher owner only)")
    public ResponseEntity<List<EnrollmentDto>> getPendingRequests(@PathVariable UUID id) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(classroomService.getPendingRequests(id, teacherId));
    }

    @PostMapping("/{id}/requests/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Approve or reject a student join request (Teacher owner only)")
    public ResponseEntity<EnrollmentDto> decideEnrollment(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @Valid @RequestBody EnrollmentDecisionDto decisionDto) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(classroomService.decideEnrollment(id, studentId, decisionDto, teacherId));
    }

    @GetMapping("/{id}/roster")
    @Operation(summary = "Get accepted roster students for a class (Teacher owner or enrolled student)")
    public ResponseEntity<List<RosterStudentDto>> getRoster(@PathVariable UUID id) {
        return ResponseEntity.ok(classroomService.getRoster(
                id,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserRole()
        ));
    }
}

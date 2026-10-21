package fyp.example.assessment.controller;

import fyp.example.assessment.service.AssessmentService;
import fyp.example.dto.assessment.AutosaveAnswersDto;
import fyp.example.dto.assessment.CreateTestDto;
import fyp.example.dto.assessment.ScorecardDto;
import fyp.example.dto.assessment.StartTestResponseDto;
import fyp.example.dto.assessment.SubmitTestRequestDto;
import fyp.example.dto.assessment.TestDetailDto;
import fyp.example.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Assessment API", description = "Test creation, scheduling, autosaving, submission, and scorecards")
@SecurityRequirement(name = "bearerAuth")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Create a test with MCQ questions (Teacher only)")
    public ResponseEntity<TestDetailDto> createTest(
            @Valid @RequestBody CreateTestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TestDetailDto created = assessmentService.createTest(dto, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @Operation(summary = "Get all tests for a class")
    public ResponseEntity<List<TestDetailDto>> getTestsForClass(
            @PathVariable UUID classId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TestDetailDto> tests = assessmentService.getTestsForClass(classId, principal);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/{testId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @Operation(summary = "Get test details by ID")
    public ResponseEntity<TestDetailDto> getTestById(
            @PathVariable UUID testId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TestDetailDto test = assessmentService.getTestById(testId, principal);
        return ResponseEntity.ok(test);
    }

    @PostMapping("/{testId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Start a test session (Student only)")
    public ResponseEntity<StartTestResponseDto> startTest(
            @PathVariable UUID testId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        StartTestResponseDto response = assessmentService.startTest(testId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{testId}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Autosave answer choices during active test (Student only)")
    public ResponseEntity<Void> autosaveAnswers(
            @PathVariable UUID testId,
            @Valid @RequestBody AutosaveAnswersDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        assessmentService.autosaveAnswers(testId, principal.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{testId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit test for auto-grading (Student only)")
    public ResponseEntity<ScorecardDto> submitTest(
            @PathVariable UUID testId,
            @RequestBody(required = false) SubmitTestRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScorecardDto scorecard = assessmentService.submitTest(testId, principal.getId(), dto);
        return ResponseEntity.ok(scorecard);
    }

    @GetMapping("/{testId}/my-submission")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get own scorecard for a test (Student only)")
    public ResponseEntity<ScorecardDto> getMySubmission(
            @PathVariable UUID testId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScorecardDto scorecard = assessmentService.getMySubmission(testId, principal.getId());
        return ResponseEntity.ok(scorecard);
    }

    @GetMapping("/{testId}/submissions")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get all student submissions for a test (Teacher only)")
    public ResponseEntity<List<ScorecardDto>> getSubmissionsForTest(
            @PathVariable UUID testId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ScorecardDto> submissions = assessmentService.getSubmissionsForTest(testId, principal.getId());
        return ResponseEntity.ok(submissions);
    }
}

package fyp.example.analytics.controller;

import fyp.example.analytics.service.AnalyticsService;
import fyp.example.dto.analytics.AiReportDto;
import fyp.example.dto.analytics.DbClassAnalyticsDto;
import fyp.example.dto.analytics.DbStudentAnalyticsDto;
import fyp.example.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & AI API", description = "Performance analytics, score trends, and AI-powered learning diagnosis")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/student/{studentId}/db-metrics")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get database-calculated performance metrics and score history for a student")
    public ResponseEntity<DbStudentAnalyticsDto> getStudentDbMetrics(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DbStudentAnalyticsDto dto = analyticsService.getStudentDbMetrics(studentId, principal);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/class/{classId}/db-metrics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get class-wide performance metrics, distribution, and topic performance (Teacher only)")
    public ResponseEntity<DbClassAnalyticsDto> getClassDbMetrics(
            @PathVariable UUID classId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DbClassAnalyticsDto dto = analyticsService.getClassDbMetrics(classId, principal);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/student/{studentId}/ai-report")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get personalized Google Gemini AI learning diagnosis for a student")
    public ResponseEntity<AiReportDto> getStudentAiReport(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AiReportDto report = analyticsService.getStudentAiReport(studentId, principal);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/class/{classId}/ai-report")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get aggregate Google Gemini AI instructional diagnosis for a class (Teacher only)")
    public ResponseEntity<AiReportDto> getClassAiReport(
            @PathVariable UUID classId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AiReportDto report = analyticsService.getClassAiReport(classId, principal);
        return ResponseEntity.ok(report);
    }
}

package fyp.example.assessment.controller;

import fyp.example.assessment.service.AssessmentService;
import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/tests")
@RequiredArgsConstructor
@Hidden
public class InternalAssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/analytics/aggregated")
    public ResponseEntity<AggregatedAnalyticsDto> getAggregatedAnalytics(
            @RequestParam UUID targetId,
            @RequestParam String type
    ) {
        AggregatedAnalyticsDto dto = assessmentService.getAggregatedAnalytics(targetId, type);
        return ResponseEntity.ok(dto);
    }
}

package fyp.example.analytics.client;

import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "assessment-service", url = "${assessment-service.url:http://localhost:8083}")
public interface AssessmentClient {

    @GetMapping("/internal/tests/analytics/aggregated")
    AggregatedAnalyticsDto getAggregatedAnalytics(
            @RequestParam("targetId") UUID targetId,
            @RequestParam("type") String type,
            @RequestHeader("X-Internal-Token") String internalToken
    );
}

package fyp.example.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fyp.example.dto.analytics.AiReportDto;
import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import fyp.example.dto.enums.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-flash-latest}")
    private String model;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    public AiReportDto generateDiagnosis(UUID targetId, ReportType reportType, AggregatedAnalyticsDto data) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("your_")) {
            log.info("Gemini API key not configured. Generating rule-based AI diagnosis for target {}", targetId);
            return generateFallbackDiagnosis(targetId, reportType, data);
        }

        try {
            String prompt = buildPrompt(targetId, reportType, data);
            String url = apiUrl + "/" + model + ":generateContent";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> requestBody = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    return parseAiResponse(targetId, reportType, text, data);
                }
            }
            log.warn("Gemini API returned empty response. Falling back to rule-based diagnosis.");
            return generateFallbackDiagnosis(targetId, reportType, data);
        } catch (Exception e) {
            log.warn("Gemini API invocation failed ({}: {}). Falling back to rule-based diagnosis.", e.getClass().getSimpleName(), e.getMessage());
            return generateFallbackDiagnosis(targetId, reportType, data);
        }
    }

    private String buildPrompt(UUID targetId, ReportType reportType, AggregatedAnalyticsDto data) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert educational analytics and pedagogical AI tutor.\n");
        if (reportType == ReportType.INDIVIDUAL_STUDENT) {
            sb.append("Analyze the following student performance metrics and generate a personalized learning diagnosis.\n\n");
            sb.append("Metrics:\n");
            sb.append("- Total Tests Attempted: ").append(data.getTotalSubmissions()).append("\n");
            sb.append("- Average Score: ").append(data.getAverageScore()).append("\n");
            sb.append("- Average Percentage: ").append(data.getAveragePercentage()).append("%\n");
            sb.append("- Highest Score: ").append(data.getHighestScore()).append("\n");
            sb.append("- Lowest Score: ").append(data.getLowestScore()).append("\n");
            sb.append("- Topic Accuracies: ").append(data.getTopicAccuracyMap()).append("\n\n");
        } else {
            sb.append("Analyze the following classroom-wide performance metrics and generate an aggregate instructional diagnosis for the teacher.\n\n");
            sb.append("Metrics:\n");
            sb.append("- Total Submissions: ").append(data.getTotalSubmissions()).append("\n");
            sb.append("- Class Average Percentage: ").append(data.getAveragePercentage()).append("%\n");
            sb.append("- Score Distribution: ").append(data.getScoreDistribution()).append("\n");
            sb.append("- Topic Accuracies: ").append(data.getTopicAccuracyMap()).append("\n\n");
        }

        sb.append("Please provide your output in four clear sections with exactly these headers:\n");
        sb.append("### SUMMARY\n<1-2 paragraph overview>\n\n");
        sb.append("### STRENGTHS\n<Key strengths and high accuracy areas>\n\n");
        sb.append("### AREAS FOR IMPROVEMENT\n<Vulnerabilities, weak topics, or common misconceptions>\n\n");
        sb.append("### RECOMMENDATIONS\n<Actionable study steps or teaching strategies>\n");
        return sb.toString();
    }

    private AiReportDto parseAiResponse(UUID targetId, ReportType reportType, String text, AggregatedAnalyticsDto data) {
        String summary = extractSection(text, "SUMMARY", "STRENGTHS");
        String strengths = extractSection(text, "STRENGTHS", "AREAS FOR IMPROVEMENT");
        String areasForImprovement = extractSection(text, "AREAS FOR IMPROVEMENT", "RECOMMENDATIONS");
        String recommendations = extractSection(text, "RECOMMENDATIONS", null);

        if (summary.isEmpty()) summary = text;
        if (strengths.isEmpty()) strengths = "Demonstrated consistent engagement across assessments.";
        if (areasForImprovement.isEmpty()) areasForImprovement = "Focus on lowest-scoring topics.";
        if (recommendations.isEmpty()) recommendations = "Review missed questions and practice foundational concepts.";

        return AiReportDto.builder()
                .id(UUID.randomUUID())
                .reportType(reportType)
                .studentId(reportType == ReportType.INDIVIDUAL_STUDENT ? targetId : null)
                .classId(reportType == ReportType.CLASS_OVERALL ? targetId : null)
                .aiSummary(summary.trim())
                .strengths(strengths.trim())
                .areasForImprovement(areasForImprovement.trim())
                .recommendations(recommendations.trim())
                .isFallback(false)
                .generatedAt(Instant.now())
                .build();
    }

    private String extractSection(String fullText, String sectionHeader, String nextSectionHeader) {
        String marker = "### " + sectionHeader;
        int start = fullText.indexOf(marker);
        if (start == -1) {
            marker = "**" + sectionHeader + "**";
            start = fullText.indexOf(marker);
        }
        if (start == -1) {
            marker = sectionHeader + ":";
            start = fullText.indexOf(marker);
        }
        if (start == -1) return "";

        start += marker.length();
        int end = fullText.length();
        if (nextSectionHeader != null) {
            int next1 = fullText.indexOf("### " + nextSectionHeader, start);
            int next2 = fullText.indexOf("**" + nextSectionHeader + "**", start);
            int next3 = fullText.indexOf(nextSectionHeader + ":", start);

            int minNext = Integer.MAX_VALUE;
            if (next1 != -1 && next1 < minNext) minNext = next1;
            if (next2 != -1 && next2 < minNext) minNext = next2;
            if (next3 != -1 && next3 < minNext) minNext = next3;

            if (minNext != Integer.MAX_VALUE) {
                end = minNext;
            }
        }

        return fullText.substring(start, end).trim();
    }

    public AiReportDto generateFallbackDiagnosis(UUID targetId, ReportType reportType, AggregatedAnalyticsDto data) {
        Map<String, Double> topicAccuracy = data.getTopicAccuracyMap() != null ? data.getTopicAccuracyMap() : Collections.emptyMap();
        List<Map.Entry<String, Double>> sortedTopics = topicAccuracy.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<String> strongTopics = sortedTopics.stream()
                .filter(e -> e.getValue() >= 70.0)
                .map(e -> e.getKey() + " (" + e.getValue() + "%)")
                .collect(Collectors.toList());

        List<String> weakTopics = sortedTopics.stream()
                .filter(e -> e.getValue() < 70.0)
                .map(e -> e.getKey() + " (" + e.getValue() + "%)")
                .collect(Collectors.toList());

        String summary;
        String strengths;
        String areas;
        String recommendations;

        double avg = data.getAveragePercentage();

        if (reportType == ReportType.INDIVIDUAL_STUDENT) {
            summary = String.format("Student has completed %d assessment(s) with an overall average score of %.1f%%. Highest score recorded is %.0f.",
                    data.getTotalSubmissions(), avg, data.getHighestScore());

            strengths = strongTopics.isEmpty()
                    ? "Consistent completion of assigned tests and steady problem-solving attempts."
                    : "Strong conceptual mastery in: " + String.join(", ", strongTopics) + ".";

            areas = weakTopics.isEmpty()
                    ? "Maintain revision across advanced problem sets to prevent knowledge decay."
                    : "Further practice required in: " + String.join(", ", weakTopics) + ".";

            recommendations = "1. Revisit missed questions from recent tests.\n2. Prioritize targeted exercises in " +
                    (weakTopics.isEmpty() ? "core modules" : weakTopics.get(0)) + ".\n3. Take regular timed mock quizzes.";
        } else {
            summary = String.format("Class assessment cohort summary across %d submission(s). The cohort average stands at %.1f%%.",
                    data.getTotalSubmissions(), avg);

            strengths = strongTopics.isEmpty()
                    ? "High participation rate and foundational topic engagement."
                    : "The cohort demonstrated high competency in: " + String.join(", ", strongTopics) + ".";

            areas = weakTopics.isEmpty()
                    ? "Continue with progressive difficulty scaling for upcoming assessments."
                    : "Common misconceptions observed in: " + String.join(", ", weakTopics) + ".";

            recommendations = "1. Schedule review lectures for " + (weakTopics.isEmpty() ? "complex concepts" : weakTopics.get(0)) +
                    ".\n2. Provide supplementary problem sets.\n3. Consider formative micro-assessments before next major exam.";
        }

        return AiReportDto.builder()
                .id(UUID.randomUUID())
                .reportType(reportType)
                .studentId(reportType == ReportType.INDIVIDUAL_STUDENT ? targetId : null)
                .classId(reportType == ReportType.CLASS_OVERALL ? targetId : null)
                .aiSummary(summary)
                .strengths(strengths)
                .areasForImprovement(areas)
                .recommendations(recommendations)
                .isFallback(true)
                .generatedAt(Instant.now())
                .build();
    }
}

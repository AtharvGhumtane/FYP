package fyp.example.analytics.service;

import fyp.example.analytics.client.AssessmentClient;
import fyp.example.analytics.client.ClassroomClient;
import fyp.example.analytics.entity.AiReport;
import fyp.example.analytics.repository.AiReportRepository;
import fyp.example.dto.analytics.AiReportDto;
import fyp.example.dto.analytics.DbClassAnalyticsDto;
import fyp.example.dto.analytics.DbStudentAnalyticsDto;
import fyp.example.dto.assessment.AggregatedAnalyticsDto;
import fyp.example.dto.classroom.ClassVerificationResponseDto;
import fyp.example.dto.enums.ReportType;
import fyp.example.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AiReportRepository aiReportRepository;
    private final AssessmentClient assessmentClient;
    private final ClassroomClient classroomClient;
    private final GeminiAiService geminiAiService;

    @Value("${app.security.internal-service-key:}")
    private String internalServiceKey;

    @Transactional(readOnly = true)
    public DbStudentAnalyticsDto getStudentDbMetrics(UUID studentId, UserPrincipal principal) {
        // Enforce student ownership: student can only view own metrics; teacher/admin can view any student
        if (principal.getRole().name().equals("ROLE_STUDENT") && !principal.getId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view another student's analytics");
        }

        AggregatedAnalyticsDto aggregated = assessmentClient.getAggregatedAnalytics(studentId, "student", internalServiceKey);

        List<DbStudentAnalyticsDto.StudentScoreHistoryPointDto> scoreHistory = new ArrayList<>();
        if (aggregated.getScoreTrend() != null) {
            for (AggregatedAnalyticsDto.ScoreTrendPointDto point : aggregated.getScoreTrend()) {
                scoreHistory.add(DbStudentAnalyticsDto.StudentScoreHistoryPointDto.builder()
                        .testTitle(point.getTestTitle())
                        .percentage(point.getPercentage())
                        .submittedAt(point.getDate())
                        .build());
            }
        }

        return DbStudentAnalyticsDto.builder()
                .studentId(studentId)
                .studentName(principal.getFullName())
                .totalTestsAttempted(aggregated.getTotalSubmissions())
                .averagePercentage(aggregated.getAveragePercentage())
                .highestPercentage(aggregated.getHighestScore())
                .scoreHistory(scoreHistory)
                .topicAccuracy(aggregated.getTopicAccuracyMap() != null ? aggregated.getTopicAccuracyMap() : new HashMap<>())
                .build();
    }

    @Transactional(readOnly = true)
    public DbClassAnalyticsDto getClassDbMetrics(UUID classId, UserPrincipal principal) {
        // Verify teacher ownership
        ClassVerificationResponseDto verification = classroomClient.verifyTeacher(classId, principal.getId(), internalServiceKey);
        if (verification == null || !verification.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view analytics for this class");
        }

        AggregatedAnalyticsDto aggregated = assessmentClient.getAggregatedAnalytics(classId, "class", internalServiceKey);

        List<UUID> roster = classroomClient.getClassRoster(classId, internalServiceKey);
        int enrolledCount = roster != null ? roster.size() : 0;

        return DbClassAnalyticsDto.builder()
                .classId(classId)
                .className(verification.getClassName() != null ? verification.getClassName() : "Class Analytics")
                .totalEnrolledStudents(enrolledCount)
                .totalTestsConducted(aggregated.getScoreTrend() != null ? aggregated.getScoreTrend().size() : 0)
                .classAveragePercentage(aggregated.getAveragePercentage())
                .scoreDistribution(aggregated.getScoreDistribution() != null ? aggregated.getScoreDistribution() : new LinkedHashMap<>())
                .topicPerformance(aggregated.getTopicAccuracyMap() != null ? aggregated.getTopicAccuracyMap() : new HashMap<>())
                .build();
    }

    @Transactional
    public AiReportDto getStudentAiReport(UUID studentId, UserPrincipal principal) {
        if (principal.getRole().name().equals("ROLE_STUDENT") && !principal.getId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view another student's AI report");
        }

        AggregatedAnalyticsDto aggregated = assessmentClient.getAggregatedAnalytics(studentId, "student", internalServiceKey);

        // Check cached report
        Optional<AiReport> cachedOpt = aiReportRepository.findTopByStudentIdOrderByGeneratedAtDesc(studentId);
        if (cachedOpt.isPresent()) {
            AiReport cached = cachedOpt.get();
            // If submission count hasn't changed, return cached report
            if (cached.getSubmissionCountSnapshot() == aggregated.getTotalSubmissions()) {
                log.info("Returning cached AI report for student {}", studentId);
                return mapToDto(cached);
            }
        }

        // Generate new report
        AiReportDto freshReport = geminiAiService.generateDiagnosis(studentId, ReportType.INDIVIDUAL_STUDENT, aggregated);

        AiReport entity = AiReport.builder()
                .id(UUID.randomUUID())
                .reportType(ReportType.INDIVIDUAL_STUDENT)
                .studentId(studentId)
                .classId(null)
                .submissionCountSnapshot(aggregated.getTotalSubmissions())
                .aiSummary(freshReport.getAiSummary())
                .strengths(freshReport.getStrengths())
                .areasForImprovement(freshReport.getAreasForImprovement())
                .recommendations(freshReport.getRecommendations())
                .isFallback(freshReport.isFallback())
                .generatedAt(Instant.now())
                .build();

        AiReport saved = aiReportRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public AiReportDto getClassAiReport(UUID classId, UserPrincipal principal) {
        ClassVerificationResponseDto verification = classroomClient.verifyTeacher(classId, principal.getId(), internalServiceKey);
        if (verification == null || !verification.isAuthorized()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view AI report for this class");
        }

        AggregatedAnalyticsDto aggregated = assessmentClient.getAggregatedAnalytics(classId, "class", internalServiceKey);

        // Check cached report
        Optional<AiReport> cachedOpt = aiReportRepository.findTopByClassIdOrderByGeneratedAtDesc(classId);
        if (cachedOpt.isPresent()) {
            AiReport cached = cachedOpt.get();
            if (cached.getSubmissionCountSnapshot() == aggregated.getTotalSubmissions()) {
                log.info("Returning cached AI report for class {}", classId);
                return mapToDto(cached);
            }
        }

        // Generate new report
        AiReportDto freshReport = geminiAiService.generateDiagnosis(classId, ReportType.CLASS_OVERALL, aggregated);

        AiReport entity = AiReport.builder()
                .id(UUID.randomUUID())
                .reportType(ReportType.CLASS_OVERALL)
                .studentId(null)
                .classId(classId)
                .submissionCountSnapshot(aggregated.getTotalSubmissions())
                .aiSummary(freshReport.getAiSummary())
                .strengths(freshReport.getStrengths())
                .areasForImprovement(freshReport.getAreasForImprovement())
                .recommendations(freshReport.getRecommendations())
                .isFallback(freshReport.isFallback())
                .generatedAt(Instant.now())
                .build();

        AiReport saved = aiReportRepository.save(entity);
        return mapToDto(saved);
    }

    private AiReportDto mapToDto(AiReport r) {
        return AiReportDto.builder()
                .id(r.getId())
                .reportType(r.getReportType())
                .studentId(r.getStudentId())
                .classId(r.getClassId())
                .aiSummary(r.getAiSummary())
                .strengths(r.getStrengths())
                .areasForImprovement(r.getAreasForImprovement())
                .recommendations(r.getRecommendations())
                .isFallback(r.isFallback())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}

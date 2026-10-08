package fyp.example.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbStudentAnalyticsDto {
    private UUID studentId;
    private String studentName;
    private int totalTestsAttempted;
    private double averagePercentage;
    private double highestPercentage;
    private List<StudentScoreHistoryPointDto> scoreHistory;
    private Map<String, Double> topicAccuracy; // Topic -> Accuracy %

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentScoreHistoryPointDto {
        private UUID testId;
        private String testTitle;
        private int score;
        private int totalMarks;
        private double percentage;
        private String submittedAt;
    }
}

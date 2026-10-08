package fyp.example.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbClassAnalyticsDto {
    private UUID classId;
    private String className;
    private int totalEnrolledStudents;
    private int totalTestsConducted;
    private double classAveragePercentage;
    private Map<String, Integer> scoreDistribution; // "0-20%", "21-40%", "41-60%", "61-80%", "81-100%" -> count
    private Map<String, Double> topicPerformance; // Topic -> Class average %
}

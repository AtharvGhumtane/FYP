package fyp.example.dto.assessment;

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
public class AggregatedAnalyticsDto {
    private UUID targetId; // studentId or classId
    private int totalSubmissions;
    private double averageScore;
    private double averagePercentage;
    private double highestScore;
    private double lowestScore;
    
    // Topic name -> Accuracy percentage (0 - 100)
    private Map<String, Double> topicAccuracyMap;

    // Test Title / Date -> Score percentage
    private List<ScoreTrendPointDto> scoreTrend;

    // Distribution: "0-20%", "21-40%", "41-60%", "61-80%", "81-100%" -> count
    private Map<String, Integer> scoreDistribution;

    // Question ID -> Miss rate percentage
    private Map<UUID, Double> questionMissRates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrendPointDto {
        private String testTitle;
        private double percentage;
        private String date;
    }
}

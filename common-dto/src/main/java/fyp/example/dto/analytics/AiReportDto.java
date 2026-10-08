package fyp.example.dto.analytics;

import fyp.example.dto.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportDto {
    private UUID id;
    private ReportType reportType;
    private UUID studentId;
    private UUID classId;
    private String aiSummary;
    private String strengths;
    private String areasForImprovement;
    private String recommendations;
    private boolean isFallback;
    private Instant generatedAt;
}

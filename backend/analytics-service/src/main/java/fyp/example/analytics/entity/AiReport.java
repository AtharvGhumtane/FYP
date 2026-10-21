package fyp.example.analytics.entity;

import fyp.example.dto.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReport {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "submission_count_snapshot", nullable = false)
    private int submissionCountSnapshot;

    @Column(name = "ai_summary", nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "areas_for_improvement", nullable = false, columnDefinition = "TEXT")
    private String areasForImprovement;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "is_fallback", nullable = false)
    private boolean isFallback;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}

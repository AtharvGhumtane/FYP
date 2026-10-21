package fyp.example.dto.assessment;

import fyp.example.dto.enums.TestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardDto {
    private UUID submissionId;
    private UUID testId;
    private String testTitle;
    private UUID studentId;
    private int score;
    private int totalMarks;
    private double percentage;
    private TestStatus status;
    private Instant startedAt;
    private Instant submittedAt;
    private List<StudentAnswerScoreDto> questions;
}

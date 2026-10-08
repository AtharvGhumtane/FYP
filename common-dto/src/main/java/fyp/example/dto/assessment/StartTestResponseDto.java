package fyp.example.dto.assessment;

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
public class StartTestResponseDto {
    private UUID submissionId;
    private UUID testId;
    private String title;
    private Instant startedAt;
    private Instant serverDeadline;
    private int durationMinutes;
    private List<QuestionDto> questions;
}

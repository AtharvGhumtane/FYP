package fyp.example.dto.assessment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDetailDto {
    private UUID id;
    private UUID classId;
    private UUID teacherId;
    private String title;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private int durationMinutes;
    private int totalMarks;
    @JsonProperty("isPublished")
    private boolean isPublished;
    private Instant createdAt;
    private List<QuestionDto> questions;
}

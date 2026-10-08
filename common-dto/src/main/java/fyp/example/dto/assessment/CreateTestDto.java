package fyp.example.dto.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateTestDto {
    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotBlank(message = "Test title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<CreateQuestionDto> questions;
}

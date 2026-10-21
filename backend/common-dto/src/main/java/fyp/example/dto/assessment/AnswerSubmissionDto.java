package fyp.example.dto.assessment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionDto {
    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @Pattern(regexp = "^[A-D]$", message = "Selected option must be A, B, C, or D")
    private String selectedOption;
}

package fyp.example.dto.assessment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionDto {
    @NotBlank(message = "Topic name is required for analytics")
    private String topic;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Option A is required")
    private String optionA;

    @NotBlank(message = "Option B is required")
    private String optionB;

    @NotBlank(message = "Option C is required")
    private String optionC;

    @NotBlank(message = "Option D is required")
    private String optionD;

    @NotBlank(message = "Correct option is required")
    @Pattern(regexp = "^[A-D]$", message = "Correct option must be A, B, C, or D")
    private String correctOption;

    @Min(value = 1, message = "Marks must be at least 1")
    private int marks;

    private String explanation;
}

package fyp.example.dto.assessment;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutosaveAnswersDto {
    @NotEmpty(message = "Answers list cannot be empty")
    private List<AnswerSubmissionDto> answers;
}

package fyp.example.dto.assessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTestRequestDto {
    private List<AnswerSubmissionDto> answers;
    private int tabSwitchCount;
}

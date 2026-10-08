package fyp.example.dto.assessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswerScoreDto {
    private UUID questionId;
    private String topic;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private String selectedOption;
    private boolean isCorrect;
    private int marksObtained;
    private int maxMarks;
    private String explanation;
}

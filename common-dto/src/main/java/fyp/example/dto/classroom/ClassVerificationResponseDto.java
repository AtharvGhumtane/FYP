package fyp.example.dto.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassVerificationResponseDto {
    private boolean isAuthorized;
    private UUID classId;
    private UUID teacherId;
    private String className;
}

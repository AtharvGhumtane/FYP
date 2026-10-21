package fyp.example.dto.classroom;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isAuthorized")
    private boolean isAuthorized;
    private UUID classId;
    private UUID teacherId;
    private String className;
}

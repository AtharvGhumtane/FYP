package fyp.example.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherKeyResponseDto {
    private UUID id;
    private String teacherEmail;
    // Plaintext key returned ONCE to admin upon creation only
    private String rawSecretKey;
    private boolean isActive;
    private String createdByAdmin;
    private Instant createdAt;
}

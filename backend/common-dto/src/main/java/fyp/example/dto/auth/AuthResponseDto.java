package fyp.example.dto.auth;

import fyp.example.dto.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String tokenType;
    private UUID userId;
    private String fullName;
    private String email;
    private UserRole role;
}

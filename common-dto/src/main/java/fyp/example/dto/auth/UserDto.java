package fyp.example.dto.auth;

import fyp.example.dto.enums.AuthProvider;
import fyp.example.dto.enums.UserRole;
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
public class UserDto {
    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private AuthProvider authProvider;
    private Instant createdAt;
}

package fyp.example.security;

import fyp.example.dto.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String VALID_32_BYTE_SECRET = "01234567890123456789012345678901_very_secure_secret_key!";
    private static final String SHORT_SECRET = "too_short_secret";

    @Test
    @DisplayName("Fails startup if JWT_SECRET is null")
    void shouldThrowExceptionWhenSecretIsNull() {
        JwtTokenProvider provider = new JwtTokenProvider(null, 1);
        assertThatThrownBy(provider::validateAndInitialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable/property is required");
    }

    @Test
    @DisplayName("Fails startup if JWT_SECRET is under 32 bytes")
    void shouldThrowExceptionWhenSecretIsUnder32Bytes() {
        JwtTokenProvider provider = new JwtTokenProvider(SHORT_SECRET, 1);
        assertThatThrownBy(provider::validateAndInitialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be at least 32 bytes");
    }

    @Test
    @DisplayName("Initializes successfully with 32+ bytes secret and produces valid claims")
    void shouldInitializeAndSignValidateTokens() {
        JwtTokenProvider provider = new JwtTokenProvider(VALID_32_BYTE_SECRET, 1);
        provider.validateAndInitialize();

        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String fullName = "Test User";
        UserRole role = UserRole.ROLE_STUDENT;

        String token = provider.generateToken(userId, email, fullName, role);
        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(provider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(provider.getFullNameFromToken(token)).isEqualTo(fullName);
        assertThat(provider.getRoleFromToken(token)).isEqualTo(role);
    }
}

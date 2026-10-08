package fyp.example.security;

import fyp.example.dto.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final String rawSecret;
    private final long expirationHours;
    private SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret:${app.jwt.secret:#{null}}}") String rawSecret,
            @Value("${jwt.expiration-hours:1}") long expirationHours) {
        this.rawSecret = rawSecret;
        this.expirationHours = expirationHours;
    }

    @PostConstruct
    public void validateAndInitialize() {
        if (rawSecret == null || rawSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable/property is required and cannot be empty.");
        }

        byte[] secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (256 bits) for HMAC-SHA security. Current length: " + secretBytes.length + " bytes.");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(UUID userId, String email, String fullName, UserRole role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("fullName", fullName)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getFullNameFromToken(String token) {
        return getClaims(token).get("fullName", String.class);
    }

    public UserRole getRoleFromToken(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return UserRole.valueOf(roleStr);
    }
}

package fyp.example.security;

import fyp.example.dto.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public class SecurityUtils {

    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }

    public static UserRole getCurrentUserRole() {
        return getCurrentUser()
                .map(UserPrincipal::getRole)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }

    public static boolean hasRole(UserRole role) {
        return getCurrentUser()
                .map(u -> u.getRole() == role)
                .orElse(false);
    }
}

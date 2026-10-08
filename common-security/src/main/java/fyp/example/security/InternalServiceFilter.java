package fyp.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalServiceFilter extends OncePerRequestFilter {

    private final String internalServiceKey;

    public InternalServiceFilter(
            @Value("${internal.service.key:secret-internal-s2s-token-key-2026}") String internalServiceKey) {
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/internal/")) {
            String incomingKey = request.getHeader("X-Internal-Token");

            if (!StringUtils.hasText(incomingKey) || !incomingKey.equals(internalServiceKey)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Invalid or missing X-Internal-Token\"}");
                return;
            }

            // Set internal service authority
            UsernamePasswordAuthenticationToken internalAuth = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(internalAuth);
        }

        filterChain.doFilter(request, response);
    }
}

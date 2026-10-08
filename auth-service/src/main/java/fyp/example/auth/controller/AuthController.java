package fyp.example.auth.controller;

import fyp.example.auth.service.AuthService;
import fyp.example.dto.auth.*;
import fyp.example.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and Google OAuth endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/student")
    @Operation(summary = "Register a new student account (server assigns ROLE_STUDENT)")
    public ResponseEntity<AuthResponseDto> registerStudent(@Valid @RequestBody StudentRegisterDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(dto));
    }

    @PostMapping("/register/teacher")
    @Operation(summary = "Register a new teacher account (requires valid secret key from registry)")
    public ResponseEntity<AuthResponseDto> registerTeacher(
            @Valid @RequestBody TeacherRegisterDto dto,
            HttpServletRequest request) {
        String ip = extractIp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTeacher(dto, ip));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/google")
    @Operation(summary = "Google OAuth2 token exchange (requires teacherSecretKey if claiming Teacher role)")
    public ResponseEntity<AuthResponseDto> googleAuth(
            @Valid @RequestBody GoogleAuthDto dto,
            HttpServletRequest request) {
        String ip = extractIp(request);
        return ResponseEntity.ok(authService.googleAuth(dto, ip));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getUserProfile(SecurityUtils.getCurrentUserId()));
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

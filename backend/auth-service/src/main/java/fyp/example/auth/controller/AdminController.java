package fyp.example.auth.controller;

import fyp.example.auth.service.TeacherKeyService;
import fyp.example.dto.auth.CreateTeacherKeyDto;
import fyp.example.dto.auth.TeacherKeyResponseDto;
import fyp.example.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Teacher key management and platform administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final TeacherKeyService teacherKeyService;

    @PostMapping("/teacher-keys")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate a new BCrypt-hashed teacher secret key (plaintext returned once)")
    public ResponseEntity<TeacherKeyResponseDto> generateTeacherKey(@Valid @RequestBody CreateTeacherKeyDto dto) {
        String adminEmail = SecurityUtils.getCurrentUser()
                .map(u -> u.getEmail())
                .orElse("system");
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherKeyService.generateTeacherKey(dto, adminEmail));
    }

    @GetMapping("/teacher-keys")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all registered teacher authorization keys (without raw secrets)")
    public ResponseEntity<List<TeacherKeyResponseDto>> getAllTeacherKeys() {
        return ResponseEntity.ok(teacherKeyService.getAllTeacherKeys());
    }

    @PatchMapping("/teacher-keys/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or revoke a teacher authorization key")
    public ResponseEntity<TeacherKeyResponseDto> toggleKeyStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        boolean isActive = body.getOrDefault("isActive", false);
        return ResponseEntity.ok(teacherKeyService.toggleKeyStatus(id, isActive));
    }
}

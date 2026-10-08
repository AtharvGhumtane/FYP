package fyp.example.auth.service;

import fyp.example.auth.entity.User;
import fyp.example.auth.repository.UserRepository;
import fyp.example.dto.enums.AuthProvider;
import fyp.example.dto.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeedingService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:${app.admin.email:#{null}}}")
    private String adminEmail;

    @Value("${admin.password:${app.admin.password:#{null}}}")
    private String adminPassword;

    @Value("${admin.name:${app.admin.name:System Administrator}}")
    private String adminName;

    public void seedAdminUser() {
        run();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            throw new IllegalStateException("ADMIN_EMAIL environment variable/property is required and cannot be empty.");
        }

        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            throw new IllegalStateException("ADMIN_PASSWORD environment variable/property is required and cannot be empty.");
        }

        String cleanAdminEmail = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanAdminEmail)) {
            log.info("Admin account already exists for email: {}", cleanAdminEmail);
            return;
        }

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .fullName(adminName.trim())
                .email(cleanAdminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ROLE_ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .createdAt(Instant.now())
                .build();

        userRepository.save(adminUser);
        log.info("Admin account initialized successfully with ID: {} and email: {}", adminUser.getId(), cleanAdminEmail);
    }
}

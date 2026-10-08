package fyp.example.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class InternalRouteBlockFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @ParameterizedTest(name = "Should block internal route variant: {0}")
    @ValueSource(strings = {
            "/internal/classes/11111111-1111-1111-1111-111111111111/verify-teacher",
            "/INTERNAL/classes/11111111-1111-1111-1111-111111111111/verify-teacher",
            "/InTeRnAl/classes/11111111-1111-1111-1111-111111111111/verify-teacher",
            "/%69%6e%74%65%72%6e%61%6c/classes/11111111-1111-1111-1111-111111111111/verify-teacher",
            "//internal//classes/11111111-1111-1111-1111-111111111111/verify-teacher",
            "/api/auth/../internal/test",
            "/internal;jsessionid=999/classes",
            "/api/tests/internal/test"
    })
    @DisplayName("Verify /internal/** blocking against all evasion variants")
    void testInternalRouteVariantsAreBlocked(String path) {
        webTestClient.get()
                .uri(path)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden")
                .jsonPath("$.message").isEqualTo("Direct external access to internal endpoints is blocked at the Gateway");
    }

    @Test
    @DisplayName("Verify Gateway Actuator Health Check returns UP")
    void testActuatorHealth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}

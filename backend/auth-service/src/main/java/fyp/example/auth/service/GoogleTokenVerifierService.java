package fyp.example.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

@Service
@Slf4j
public class GoogleTokenVerifierService {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(
            @Value("${google.client-id:dummy-client-id.apps.googleusercontent.com}") String clientId) {
        this.clientId = clientId;

        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );

        if (clientId != null && !clientId.isBlank() && !clientId.equals("dummy-client-id.apps.googleusercontent.com")) {
            builder.setAudience(Collections.singletonList(clientId));
        }

        this.verifier = builder.build();
    }

    public GoogleUserInfo verifyIdToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google ID token cannot be empty");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Invalid Google ID token supplied");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Google account email is not verified: {}", payload.getEmail());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google account email is not verified by Google");
            }

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            return new GoogleUserInfo(email.toLowerCase(), name);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google ID token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to verify Google token: " + e.getMessage());
        }
    }

    public record GoogleUserInfo(String email, String fullName) {}
}

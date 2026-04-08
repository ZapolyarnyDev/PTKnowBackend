package ptknow.service.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import ptknow.exception.credentials.InvalidCredentialsException;
import ptknow.properties.RecaptchaProperties;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecaptchaVerificationService {

    RecaptchaProperties recaptchaProperties;
    RestClient restClient = RestClient.create();

    public void verifyForLogin(String token) {
        verify(token, "login");
    }

    public void verifyForRegistration(String token) {
        verify(token, "register");
    }

    public void verify(String token, String expectedAction) {
        if (!recaptchaProperties.isEnabled()) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException("Recaptcha token is required");
        }

        String secretKey = recaptchaProperties.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Recaptcha secret key is not configured");
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("secret", secretKey);
        form.add("response", token);

        VerifyResponse response = restClient.post()
                .uri(recaptchaProperties.getVerifyUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(VerifyResponse.class);

        if (response == null || !Boolean.TRUE.equals(response.success())) {
            throw new InvalidCredentialsException("Recaptcha verification failed");
        }

        if (expectedAction != null && response.action() != null && !expectedAction.equals(response.action())) {
            throw new InvalidCredentialsException("Recaptcha action mismatch");
        }

        Double score = response.score();
        if (score != null && score < recaptchaProperties.getScoreThreshold()) {
            throw new InvalidCredentialsException("Recaptcha score is too low");
        }
    }

    private record VerifyResponse(
            Boolean success,
            Double score,
            String action,
            List<String> errorCodes
    ) {}
}

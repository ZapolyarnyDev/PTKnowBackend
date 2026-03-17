package ptknow.api.user;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ptknow.jwt.JwtTokens;
import ptknow.service.auth.JwtService;

@RestController
@RequestMapping("/v0/token")
@RequiredArgsConstructor
@Validated
public class TokenController {

    private final JwtService jwtService;

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(
            @CookieValue("refreshToken") @NotBlank(message = "Refresh token is required") String refreshToken
    ) {
        JwtTokens tokens = jwtService.refresh(refreshToken);

        ResponseCookie cookie = jwtService.tokenToCookie("/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }
}

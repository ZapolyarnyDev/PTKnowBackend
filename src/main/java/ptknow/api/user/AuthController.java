package ptknow.api.user;

import ptknow.dto.auth.LoginDTO;
import ptknow.dto.auth.RegistrationDTO;
import ptknow.model.auth.Auth;
import ptknow.jwt.JwtTokens;
import ptknow.service.auth.AuthService;
import ptknow.service.auth.JwtService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v0/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;
    JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationDTO registrationDTO) {
        Auth entity = authService.register(registrationDTO);
        JwtTokens tokens = jwtService.generateTokenPair(entity);

        ResponseCookie cookie = jwtService.tokenToCookie("/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        Auth entity = authService.authenticate(loginDTO);
        JwtTokens tokens = jwtService.generateTokenPair(entity);

        ResponseCookie cookie = jwtService.tokenToCookie("/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Auth user) {
        jwtService.invalidateUserTokens(user);

        ResponseCookie cookie = jwtService.deleteRefreshCookie("/v0/token/refresh");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

}


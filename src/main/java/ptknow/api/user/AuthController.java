package ptknow.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ptknow.api.exception.ApiError;
import ptknow.config.openapi.OpenApiExamples;

@RestController
@RequestMapping("/api/v0/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Аутентификация", description = "Регистрация, вход и выход. Access token возвращается в теле ответа, refresh token устанавливается в HttpOnly-cookie.")
public class AuthController {

    AuthService authService;
    JwtService jwtService;

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация локального пользователя",
            description = "Создаёт локальную учётную запись, возвращает access token в теле ответа и устанавливает refresh token в cookie. Предварительная аутентификация не требуется."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Регистрация выполнена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = OpenApiExamples.ACCESS_TOKEN))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationDTO registrationDTO) {
        Auth entity = authService.register(registrationDTO);
        JwtTokens tokens = jwtService.generateTokenPair(entity);

        ResponseCookie cookie = jwtService.tokenToCookie("/api/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Вход по email и паролю",
            description = "Аутентифицирует локального пользователя, возвращает access token в теле ответа и устанавливает refresh token в cookie. Предварительная аутентификация не требуется."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аутентификация выполнена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = OpenApiExamples.ACCESS_TOKEN))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "403", description = "Неверные учётные данные или пользователь заблокирован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        Auth entity = authService.authenticate(loginDTO);
        JwtTokens tokens = jwtService.generateTokenPair(entity);

        ResponseCookie cookie = jwtService.tokenToCookie("/api/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Выход текущего пользователя",
            description = "Инвалидирует активные refresh token пользователя и очищает refresh cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Выход выполнен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> logout(@AuthenticationPrincipal Auth user) {
        jwtService.logout(user);

        ResponseCookie cookie = jwtService.deleteRefreshCookie("/api/v0/token/refresh");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

}


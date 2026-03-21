package ptknow.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ptknow.api.exception.ApiError;
import ptknow.config.openapi.OpenApiExamples;
import ptknow.jwt.JwtTokens;
import ptknow.service.auth.JwtService;

@RestController
@RequestMapping("/v0/token")
@RequiredArgsConstructor
@Validated
@Tag(name = "Аутентификация", description = "Жизненный цикл токенов")
public class TokenController {

    private final JwtService jwtService;

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновить access token",
            description = "Использует refresh token из HttpOnly-cookie с именем refreshToken, ротирует его и возвращает новый access token в теле ответа. Bearer-аутентификация не требуется."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен обновлён",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = OpenApiExamples.ACCESS_TOKEN))),
            @ApiResponse(responseCode = "400", description = "Refresh token cookie отсутствует или невалиден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "403", description = "Refresh token истёк, отозван или принадлежит заблокированному пользователю",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<String> refresh(
            @Parameter(description = "Refresh token из HttpOnly-cookie", required = true, example = "refresh-token-cookie-value")
            @CookieValue("refreshToken") @NotBlank(message = "Refresh token is required") String refreshToken
    ) {
        JwtTokens tokens = jwtService.refresh(refreshToken);

        ResponseCookie cookie = jwtService.tokenToCookie("/v0/token/refresh", tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.accessToken());
    }
}

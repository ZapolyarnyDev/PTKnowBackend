package ptknow.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.exception.token.InvalidTokenException;
import ptknow.exception.token.TokenNotFoundException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;
import ptknow.model.token.RefreshToken;
import ptknow.properties.JwtProperties;
import ptknow.repository.auth.RefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    JwtEncoder jwtEncoder;

    @Mock
    RefreshTokenRepository tokenRepository;

    JwtProperties properties;

    @InjectMocks
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setIssuer("ptknow-test");
        properties.setKey("12345678901234567890123456789012");
        properties.setAccessTokenExpiration(Duration.ofMinutes(15));
        properties.setRefreshTokenExpiration(Duration.ofDays(7));
        properties.setRefreshCookieSecure(false);
        properties.setRefreshCookieSameSite("Lax");

        jwtService = new JwtService(properties, jwtEncoder, tokenRepository);
    }

    @Test
    void generateTokenPairShouldStoreRefreshTokenHashInsteadOfPlainToken() {
        Auth user = auth(Role.STUDENT, UserStatus.ACTIVE);
        mockEncoderSequence("access-token", "refresh-token");

        var tokens = jwtService.generateTokenPair(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        RefreshToken saved = tokenCaptor.getValue();
        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        assertNotEquals("refresh-token", saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertEquals(user, saved.getUser());
    }

    @Test
    void refreshShouldInvalidateCurrentTokenAndGenerateNewPair() {
        Auth user = auth(Role.STUDENT, UserStatus.ACTIVE);
        RefreshToken stored = RefreshToken.builder()
                .tokenHash("hash")
                .user(user)
                .expireDate(Instant.now().plus(Duration.ofDays(1)))
                .valid(true)
                .build();
        ReflectionTestUtils.setField(stored, "id", 10L);

        mockEncoderSequence("new-access-token", "new-refresh-token");
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(stored));

        var result = jwtService.refresh("incoming-refresh-token");

        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
        assertFalse(stored.isValid());
        verify(tokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshShouldThrowWhenTokenNotFound() {
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> jwtService.refresh("missing-token"));
    }

    @Test
    void refreshShouldThrowWhenTokenExpiredOrInvalid() {
        Auth user = auth(Role.STUDENT, UserStatus.ACTIVE);
        RefreshToken stored = RefreshToken.builder()
                .tokenHash("hash")
                .user(user)
                .expireDate(Instant.now().minusSeconds(1))
                .valid(true)
                .build();

        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(stored));

        assertThrows(InvalidTokenException.class, () -> jwtService.refresh("expired-token"));
        assertFalse(stored.isValid());
        verify(tokenRepository).save(stored);
    }

    @Test
    void refreshShouldThrowWhenUserIsBlocked() {
        Auth user = auth(Role.STUDENT, UserStatus.BLOCKED);
        RefreshToken stored = RefreshToken.builder()
                .tokenHash("hash")
                .user(user)
                .expireDate(Instant.now().plus(Duration.ofDays(1)))
                .valid(true)
                .build();

        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(stored));
        when(tokenRepository.findAllByUserAndValidIsTrueAndExpireDateAfter(eq(user), any())).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> jwtService.refresh("blocked-user-token"));
        assertFalse(stored.isValid());
        verify(tokenRepository).save(stored);
        verify(tokenRepository).findAllByUserAndValidIsTrueAndExpireDateAfter(eq(user), any());
    }

    @Test
    void invalidateUserTokensShouldMarkEveryValidTokenAsInvalid() {
        Auth user = auth(Role.STUDENT, UserStatus.ACTIVE);
        RefreshToken first = RefreshToken.builder()
                .tokenHash("hash-1")
                .user(user)
                .expireDate(Instant.now().plus(Duration.ofDays(1)))
                .valid(true)
                .build();
        RefreshToken second = RefreshToken.builder()
                .tokenHash("hash-2")
                .user(user)
                .expireDate(Instant.now().plus(Duration.ofDays(1)))
                .valid(true)
                .build();

        when(tokenRepository.findAllByUserAndValidIsTrueAndExpireDateAfter(any(), any()))
                .thenReturn(List.of(first, second));

        jwtService.invalidateUserTokens(user);

        assertFalse(first.isValid());
        assertFalse(second.isValid());
        verify(tokenRepository).saveAll(List.of(first, second));
    }

    @Test
    void logoutShouldInvalidateAllUserTokens() {
        Auth user = auth(Role.STUDENT, UserStatus.ACTIVE);
        when(tokenRepository.findAllByUserAndValidIsTrueAndExpireDateAfter(eq(user), any())).thenReturn(List.of());

        jwtService.logout(user);

        verify(tokenRepository).findAllByUserAndValidIsTrueAndExpireDateAfter(eq(user), any());
    }

    @Test
    void logoutShouldRejectMissingUser() {
        assertThrows(AccessDeniedException.class, () -> jwtService.logout(null));
    }

    @Test
    void tokenToCookieShouldBuildHttpOnlyCookieWithConfiguredFlags() {
        ResponseCookie cookie = jwtService.tokenToCookie("/v0/token/refresh", "refresh-token");

        assertEquals("refreshToken", cookie.getName());
        assertEquals("refresh-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/v0/token/refresh", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());
    }

    private void mockEncoderSequence(String... tokenValues) {
        Jwt[] tokens = new Jwt[tokenValues.length];
        for (int i = 0; i < tokenValues.length; i++) {
            tokens[i] = Jwt.withTokenValue(tokenValues[i])
                    .header("alg", "HS256")
                    .subject("subject")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();
        }
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(tokens[0], java.util.Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private Auth auth(Role role, UserStatus status) {
        Auth auth = Auth.builder()
                .email(UUID.randomUUID() + "@test.local")
                .password("password")
                .role(role)
                .build();
        auth.setStatus(status);
        ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        return auth;
    }
}

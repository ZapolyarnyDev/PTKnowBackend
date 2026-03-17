package ptknow.service.auth;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptknow.exception.token.InvalidTokenException;
import ptknow.exception.token.TokenNotFoundException;
import ptknow.jwt.ClaimType;
import ptknow.jwt.JwtClaim;
import ptknow.jwt.JwtTokens;
import ptknow.model.auth.Auth;
import ptknow.model.token.RefreshToken;
import ptknow.properties.JwtProperties;
import ptknow.repository.auth.RefreshTokenRepository;

import java.time.Instant;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class JwtService {

    static final String REFRESH_COOKIE_NAME = "refreshToken";

    JwtProperties properties;
    JwtEncoder jwtEncoder;
    RefreshTokenRepository tokenRepository;

    private String generateAccessToken(Auth user) {
        var now = Instant.now();
        var claimSet = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiresAt(properties.getAccessTokenExpiryInstant())
                .subject(user.getUsername())
                .claim(JwtClaim.TYPE.getName(), ClaimType.ACCESS.getName())
                .claim(JwtClaim.ROLE.getName(), user.getRole().authorityName())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claimSet)).getTokenValue();
    }

    private String generateRefreshToken(Auth user) {
        var now = Instant.now();
        Instant expiresAt = properties.getRefreshTokenExpiryInstant();

        var claimSet = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim(JwtClaim.TYPE.getName(), ClaimType.REFRESH.getName())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claimSet)).getTokenValue();

        var entity = RefreshToken.builder()
                .token(token)
                .expireDate(expiresAt)
                .user(user)
                .build();

        tokenRepository.save(entity);
        return token;
    }

    public JwtTokens generateTokenPair(Auth entity) {
        return new JwtTokens(generateAccessToken(entity), generateRefreshToken(entity));
    }

    @Transactional
    public JwtTokens refresh(String refreshToken) throws TokenNotFoundException, InvalidTokenException {
        RefreshToken entity = findToken(refreshToken);

        if (!isValid(entity))
            throw new InvalidTokenException();
        if (!entity.getUser().isEnabled())
            throw new AccessDeniedException("User account is blocked");

        entity.setValid(false);
        log.info("Refresh token invalidated. id={}, userId={}", entity.getId(), entity.getUser().getId());

        tokenRepository.save(entity);
        return generateTokenPair(entity.getUser());
    }

    public ResponseCookie tokenToCookie(String cookiePath, String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(properties.getRefreshCookieSecure())
                .maxAge(properties.getRefreshTokenExpiration())
                .sameSite(properties.getRefreshCookieSameSite())
                .path(cookiePath)
                .build();
    }

    public ResponseCookie deleteRefreshCookie(String cookiePath) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.getRefreshCookieSecure())
                .maxAge(0)
                .sameSite(properties.getRefreshCookieSameSite())
                .path(cookiePath)
                .build();
    }

    public boolean isValid(RefreshToken token) {
        return token.isValid() && token.getExpireDate().isAfter(Instant.now());
    }

    @Transactional
    public void invalidateUserTokens(Auth user) {
        var tokens = tokenRepository.findAllByUserAndValidIsTrueAndExpireDateAfter(user, Instant.now());

        for (var token : tokens) {
            token.setValid(false);
        }

        tokenRepository.saveAll(tokens);
        log.info("Invalidated {} refresh tokens for userId={}", tokens.size(), user.getId());
    }

    private RefreshToken findToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(TokenNotFoundException::new);
    }
}

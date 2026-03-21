package ptknow.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ptknow.config.security.RestAuthenticationEntryPoint;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;
import ptknow.service.auth.AuthService;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    JwtDecoder jwtDecoder;

    @Mock
    AuthService authService;

    @Mock
    RestAuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void shouldRejectBlockedUserAccessToken() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtDecoder, authService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v0/profile");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtDecoder.decode("access-token")).thenReturn(accessJwt("user@test.local"));
        when(authService.loadUserByUsername("user@test.local")).thenReturn(auth(UserStatus.BLOCKED));

        filter.doFilter(request, response, new MockFilterChain());

        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectTokenWithWrongType() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtDecoder, authService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v0/profile");
        request.addHeader("Authorization", "Bearer refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Jwt jwt = Jwt.withTokenValue("refresh-token")
                .header("alg", "HS256")
                .subject("user@test.local")
                .claim("type", "refresh")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(jwtDecoder.decode("refresh-token")).thenReturn(jwt);

        filter.doFilter(request, response, new MockFilterChain());

        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(authService, never()).loadUserByUsername(any());
    }

    @Test
    void shouldAuthenticateValidAccessToken() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtDecoder, authService, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v0/profile");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Auth user = auth(UserStatus.ACTIVE);

        when(jwtDecoder.decode("access-token")).thenReturn(accessJwt("user@test.local"));
        when(authService.loadUserByUsername("user@test.local")).thenReturn(user);

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        SecurityContextHolder.clearContext();
    }

    private Jwt accessJwt(String subject) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("type", "access")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private Auth auth(UserStatus status) {
        Auth auth = Auth.builder()
                .email("user@test.local")
                .password("password")
                .role(Role.STUDENT)
                .build();
        auth.setStatus(status);
        org.springframework.test.util.ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        return auth;
    }
}

package ptknow.filter;

import ptknow.model.auth.Auth;
import ptknow.config.security.RestAuthenticationEntryPoint;
import ptknow.jwt.JwtClaim;
import ptknow.service.auth.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthFilter extends OncePerRequestFilter {

    JwtDecoder jwtDecoder;
    AuthService authService;
    RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Jwt jwt = jwtDecoder.decode(token);

            if(!jwt.getClaim(JwtClaim.TYPE.getName()).equals("access")) {
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new BadCredentialsException("Invalid token type")
                );
                return;
            }

            String email = jwt.getSubject();
            Auth entity = authService.loadUserByUsername(email);
            if (!entity.isEnabled()) {
                throw new BadCredentialsException("User account is blocked");
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    entity,
                    null,
                    entity.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (RuntimeException e) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid access token", e)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}


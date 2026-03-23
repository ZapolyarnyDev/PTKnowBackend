package ptknow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ptknow.api.exception.ApiErrorResponseWriter;
import ptknow.properties.AuthRateLimitProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static String LOGIN_PATH = "/api/v0/auth/login";
    static String REGISTER_PATH = "/api/v0/auth/register";
    static String REFRESH_PATH = "/api/v0/token/refresh";

    AuthRateLimitProperties properties;
    ApiErrorResponseWriter errorResponseWriter;

    Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = resolveRule(request.getServletPath());
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getServletPath() + "|" + resolveClientIp(request);
        Counter counter = counters.computeIfAbsent(key, k -> new Counter());

        long now = System.currentTimeMillis();
        long retryAfterSeconds = counter.tryAcquire(now, rule);

        if (retryAfterSeconds >= 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            errorResponseWriter.write(
                    request,
                    response,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "too_many_requests",
                    "Too many requests. Try again later"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Rule resolveRule(String path) {
        if (LOGIN_PATH.equals(path)) {
            return ruleOf(properties.getLogin());
        }
        if (REGISTER_PATH.equals(path)) {
            return ruleOf(properties.getRegister());
        }
        if (REFRESH_PATH.equals(path)) {
            return ruleOf(properties.getRefresh());
        }
        return null;
    }

    private Rule ruleOf(AuthRateLimitProperties.Rule raw) {
        return new Rule(raw.getMaxRequests(), raw.getWindow());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header == null || header.isBlank()) {
            return request.getRemoteAddr();
        }
        int idx = header.indexOf(',');
        return (idx > 0 ? header.substring(0, idx) : header).trim();
    }

    record Rule(int maxRequests, Duration window) {
    }

    static class Counter {
        long windowStartMs = 0;
        int used = 0;

        synchronized long tryAcquire(long now, Rule rule) {
            long windowMs = rule.window().toMillis();

            if (windowStartMs == 0 || now - windowStartMs >= windowMs) {
                windowStartMs = now;
                used = 1;
                return -1;
            }

            if (used >= rule.maxRequests()) {
                long retryMs = windowMs - (now - windowStartMs);
                long retrySec = (long) Math.ceil(retryMs / 1000.0);
                return Math.max(1, retrySec);
            }

            used++;
            return -1;
        }
    }
}

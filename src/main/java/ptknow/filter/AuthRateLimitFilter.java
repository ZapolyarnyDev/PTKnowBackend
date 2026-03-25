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
import org.springframework.util.AntPathMatcher;
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

    static String LOGIN_PATH = "/api/v1/auth/login";
    static String REGISTER_PATH = "/api/v1/auth/register";
    static String REFRESH_PATH = "/api/v1/token/refresh";
    static String COURSE_LIST_PATH = "/api/v1/course";
    static String COURSE_ID_PATH = "/api/v1/course/id/*";
    static String COURSE_HANDLE_PATH = "/api/v1/course/handle/*";
    static String LESSON_ID_PATH = "/api/v1/lessons/*";
    static String LESSON_COURSE_PATH = "/api/v1/lessons/course/*";
    static String PROFILE_SEARCH_PATH = "/api/v1/profile/search";

    AuthRateLimitProperties properties;
    ApiErrorResponseWriter errorResponseWriter;

    Map<String, Counter> counters = new ConcurrentHashMap<>();
    AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            filterChain.doFilter(request, response);
            return;
        }

        ResolvedRule resolvedRule = resolveRule(request);
        Rule rule = resolvedRule != null ? resolvedRule.rule() : null;
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolvedRule.key() + "|" + resolveClientIp(request);
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

    private ResolvedRule resolveRule(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path)) {
            return new ResolvedRule("auth:login", ruleOf(properties.getLogin()));
        }
        if ("POST".equalsIgnoreCase(method) && REGISTER_PATH.equals(path)) {
            return new ResolvedRule("auth:register", ruleOf(properties.getRegister()));
        }
        if ("POST".equalsIgnoreCase(method) && REFRESH_PATH.equals(path)) {
            return new ResolvedRule("auth:refresh", ruleOf(properties.getRefresh()));
        }

        if (!"GET".equalsIgnoreCase(method) || isAuthenticatedRequest(request)) {
            return null;
        }

        if (COURSE_LIST_PATH.equals(path)) {
            return new ResolvedRule("public:course-list", ruleOf(properties.getPublicCourseList()));
        }
        if (pathMatcher.match(COURSE_ID_PATH, path) || pathMatcher.match(COURSE_HANDLE_PATH, path)) {
            return new ResolvedRule("public:course-read", ruleOf(properties.getPublicCourseRead()));
        }
        if (pathMatcher.match(LESSON_ID_PATH, path) || pathMatcher.match(LESSON_COURSE_PATH, path)) {
            return new ResolvedRule("public:lesson-read", ruleOf(properties.getPublicLessonRead()));
        }
        if (PROFILE_SEARCH_PATH.equals(path)) {
            return new ResolvedRule("public:profile-search", ruleOf(properties.getPublicProfileSearch()));
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

    private boolean isAuthenticatedRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && !authorization.isBlank();
    }

    record Rule(int maxRequests, Duration window) {
    }

    record ResolvedRule(String key, Rule rule) {
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

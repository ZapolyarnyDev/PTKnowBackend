package ptknow.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ptknow.api.exception.ApiErrorResponseWriter;
import ptknow.properties.AuthRateLimitProperties;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitFilterTest {

    AuthRateLimitProperties properties;
    AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new AuthRateLimitProperties();
        properties.setEnabled(true);
        properties.getLogin().setMaxRequests(1);
        properties.getPublicCourseList().setMaxRequests(1);
        properties.getPublicProfileSearch().setMaxRequests(1);
        filter = new AuthRateLimitFilter(
                properties,
                new ApiErrorResponseWriter(new ObjectMapper())
        );
    }

    @Test
    void shouldReturnTooManyRequestsWhenLimitIsExceeded() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletRequest firstRequest = request("/api/v1/auth/login", "127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, chain);

        MockHttpServletRequest secondRequest = request("/api/v1/auth/login", "127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertTrue(secondResponse.getContentAsString().contains("\"code\":\"too_many_requests\""));
        assertEquals("60", secondResponse.getHeader("Retry-After"));
    }

    @Test
    void shouldSkipNonProtectedPaths() throws Exception {
        MockHttpServletRequest request = request("/api/v1/profile", "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRateLimitAnonymousCourseCatalogRequests() throws Exception {
        MockHttpServletRequest firstRequest = getRequest("/api/v1/course", "127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = getRequest("/api/v1/course", "127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void shouldSkipPublicReadLimiterForAuthenticatedRequest() throws Exception {
        MockHttpServletRequest firstRequest = getRequest("/api/v1/course", "127.0.0.1");
        firstRequest.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = getRequest("/api/v1/course", "127.0.0.1");
        secondRequest.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
    }

    private MockHttpServletRequest request(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletRequest getRequest(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}

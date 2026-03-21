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
        filter = new AuthRateLimitFilter(
                properties,
                new ApiErrorResponseWriter(new ObjectMapper())
        );
    }

    @Test
    void shouldReturnTooManyRequestsWhenLimitIsExceeded() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletRequest firstRequest = request("/v0/auth/login", "127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, chain);

        MockHttpServletRequest secondRequest = request("/v0/auth/login", "127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertTrue(secondResponse.getContentAsString().contains("\"code\":\"too_many_requests\""));
        assertEquals("60", secondResponse.getHeader("Retry-After"));
    }

    @Test
    void shouldSkipNonProtectedPaths() throws Exception {
        MockHttpServletRequest request = request("/v0/profile", "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest request(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}

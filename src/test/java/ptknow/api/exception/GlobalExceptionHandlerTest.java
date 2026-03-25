package ptknow.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHideInternalDetailsForDataIntegrityViolation() {
        HttpServletRequest request = request("/api/v1/auth/register");

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint auth_data_email_key"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("resource_already_exists", response.getBody().code());
        assertEquals("Request conflicts with current data state", response.getBody().message());
        assertFalse(response.getBody().message().contains("auth_data"));
    }

    @Test
    void shouldReturnSafeBadRequestMessageForUnreadableBody() {
        HttpServletRequest request = request("/api/v1/course");

        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new HttpMessageNotReadableException("JSON parse error: internal details", inputMessage()),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad_request", response.getBody().code());
        assertEquals("Malformed request body", response.getBody().message());
    }

    private HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }

    private HttpInputMessage inputMessage() {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return new org.springframework.http.HttpHeaders();
            }
        };
    }
}

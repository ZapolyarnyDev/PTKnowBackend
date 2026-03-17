package ptknow.exception.token;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Refresh token is invalid");
    }
}

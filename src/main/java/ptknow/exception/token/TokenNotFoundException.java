package ptknow.exception.token;

public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException() {
        super("Refresh token not found");
    }
}

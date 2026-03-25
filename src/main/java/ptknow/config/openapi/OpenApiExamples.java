package ptknow.config.openapi;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidHlwZSI6ImFjY2VzcyJ9.signature";
    public static final String UUID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String COURSE_ID = "42";
    public static final String LESSON_ID = "73";
    public static final String COURSE_HANDLE = "java-backend-basics";
    public static final String PROFILE_HANDLE = "ivan-petrov";
    public static final String API_ERROR = """
            {
              "timestamp": "2026-03-22T01:15:00Z",
              "error": "Forbidden",
              "code": "access_denied",
              "path": "/api/v1/course/42/teachers",
              "message": "Access is denied",
              "fieldErrors": {}
            }
            """;
    public static final String VALIDATION_ERROR = """
            {
              "timestamp": "2026-03-22T01:15:00Z",
              "error": "Bad Request",
              "code": "validation_failed",
              "path": "/api/v1/auth/register",
              "message": "Request validation failed",
              "fieldErrors": {
                "email": "must be a well-formed email address"
              }
            }
            """;
}

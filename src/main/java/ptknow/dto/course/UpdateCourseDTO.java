package ptknow.dto.course;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateCourseDTO(
        @Pattern(regexp = ".*\\S.*", message = "Course name must not be blank")
        @Size(max = 255, message = "Course name must be at most 255 characters")
        String name,
        @Size(max = 2000, message = "Course description must be at most 2000 characters")
        String description,
        Set<String> tags,
        @Positive(message = "Course max users amount must be positive")
        Integer maxUsersAmount
) {
}

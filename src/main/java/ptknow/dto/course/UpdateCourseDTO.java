package ptknow.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(name = "UpdateCourseDTO", description = "Partial course update payload")
public record UpdateCourseDTO(
        @Schema(example = "Advanced Java Backend")
        @Pattern(regexp = ".*\\S.*", message = "Course name must not be blank")
        @Size(max = 255, message = "Course name must be at most 255 characters")
        String name,
        @Schema(example = "Updated course description")
        @Size(max = 2000, message = "Course description must be at most 2000 characters")
        String description,
        @ArraySchema(schema = @Schema(example = "spring"))
        Set<String> tags,
        @Schema(example = "25")
        @Positive(message = "Course max users amount must be positive")
        Integer maxUsersAmount
) {
}

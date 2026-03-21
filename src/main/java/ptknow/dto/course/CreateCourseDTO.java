package ptknow.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(name = "CreateCourseDTO", description = "Payload for course creation")
public record CreateCourseDTO(
        @Schema(example = "Java Backend Basics")
        @NotBlank String name,
        @Schema(example = "Spring Boot course for beginners with lessons and materials.")
        @NotBlank String description,
        @ArraySchema(schema = @Schema(example = "java"))
        @NotEmpty Set<String> tags
) { }


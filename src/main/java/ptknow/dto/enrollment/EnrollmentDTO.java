package ptknow.dto.enrollment;

import ptknow.dto.shared.CourseSummaryDTO;
import ptknow.dto.shared.UserSummaryDTO;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentDTO(
        Long id,
        UUID userId,
        Long courseId,
        Instant since,
        UserSummaryDTO user,
        CourseSummaryDTO course
) {
}

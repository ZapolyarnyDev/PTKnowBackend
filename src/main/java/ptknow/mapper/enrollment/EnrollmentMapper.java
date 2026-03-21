package ptknow.mapper.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ptknow.dto.enrollment.EnrollmentDTO;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.enrollment.Enrollment;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollmentMapper {

    private final ApiViewMapper apiViewMapper;

    public EnrollmentDTO toDTOFromEntity(Enrollment enrollment) {
        return new EnrollmentDTO(
                enrollment.getId(),
                enrollment.getUser().getId(),
                enrollment.getCourse().getId(),
                enrollment.getEnrollSince(),
                apiViewMapper.toUserSummary(enrollment.getUser()),
                apiViewMapper.toCourseSummary(enrollment.getCourse())
        );
    }

    public List<EnrollmentDTO> mapEntityList(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(this::toDTOFromEntity)
                .toList();
    }
}

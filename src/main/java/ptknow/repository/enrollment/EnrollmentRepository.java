package ptknow.repository.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ptknow.model.course.Course;
import ptknow.model.enrollment.Enrollment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUser_IdAndCourse_Id(UUID userId, Long courseId);

    Optional<Enrollment> findByUser_IdAndCourse_Id(UUID userId, Long courseId);

    void deleteByUser_IdAndCourse_Id(UUID userId, Long courseId);

    List<Enrollment> findAllByCourse_Id(Long courseId);

    List<Enrollment> findAllByUser_Id(UUID userId);

    @Query("select e.course.id, count(e) from Enrollment e where e.course.id in :courseIds group by e.course.id")
    List<Object[]> countByCourseIds(@Param("courseIds") Set<Long> courseIds);

    @Query("""
            select c from Enrollment e
            join e.course c
            left join fetch c.preview
            where e.user.id = :userId
            order by c.name asc
            """)
    List<Course> findAllCourseViewsByUserId(@Param("userId") UUID userId);

    @Query("""
            select c from Enrollment e
            join e.course c
            left join fetch c.preview
            where e.user.id = :userId
              and c.state = ptknow.model.course.CourseState.PUBLISHED
            order by c.name asc
            """)
    List<Course> findAllPublishedCourseViewsByUserId(@Param("userId") UUID userId);

    int countByCourse_Id(Long courseId);
}
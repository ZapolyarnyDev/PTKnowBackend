package ptknow.repository.lesson;

import ptknow.model.lesson.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    List<Lesson> getAllByCourse_Id(Long courseId);
    @Query("select l.course.id, count(l) from Lesson l where l.course.id in :courseIds group by l.course.id")
    List<Object[]> countByCourseIds(@Param("courseIds") Set<Long> courseIds);

    boolean existsByIdAndOwner_Id(Long id, UUID ownerId);
}


package ptknow.repository.lesson;

import ptknow.model.lesson.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    List<Lesson> getAllByCourse_Id(Long courseId);

    boolean existsByIdAndOwner_Id(Long id, UUID ownerId);
}


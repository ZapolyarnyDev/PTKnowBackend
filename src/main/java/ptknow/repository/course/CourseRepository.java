package ptknow.repository.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    Optional<Course> findByName(String name);
    Optional<Course> findByHandle(String handle);
    boolean existsByName(String name);
    boolean existsByHandle(String handle);
    List<Course> findAllByState(CourseState state);
    int countByCourseTagsContains(CourseTag courseTagEntity);
    boolean existsByIdAndOwner_Id(Long id, UUID ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

}


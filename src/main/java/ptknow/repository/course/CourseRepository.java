package ptknow.repository.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    @EntityGraph(attributePaths = {"courseTags", "preview", "owner", "owner.profile", "owner.profile.avatar", "editors", "editors.profile", "editors.profile.avatar"})
    @Query("select distinct c from Course c where c.id in :ids")
    List<Course> findAllListViewByIdIn(@Param("ids") Set<Long> ids);

    @EntityGraph(attributePaths = {"courseTags", "preview", "owner", "owner.profile", "owner.profile.avatar", "editors", "editors.profile", "editors.profile.avatar"})
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findViewById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"courseTags", "preview", "owner", "owner.profile", "owner.profile.avatar", "editors", "editors.profile", "editors.profile.avatar"})
    @Query("select c from Course c where c.handle = :handle")
    Optional<Course> findViewByHandle(@Param("handle") String handle);

    @Query("""
            select distinct c from Course c
            left join c.editors e
            left join fetch c.preview
            where c.owner.id = :userId or e.id = :userId
            order by c.name asc
            """)
    List<Course> findAllTeachingViewByUserId(@Param("userId") UUID userId);

    @Query("""
            select distinct c from Course c
            left join c.editors e
            left join fetch c.preview
            where (c.owner.id = :userId or e.id = :userId)
              and c.state = ptknow.model.course.CourseState.PUBLISHED
            order by c.name asc
            """)
    List<Course> findAllPublishedTeachingViewByUserId(@Param("userId") UUID userId);
}
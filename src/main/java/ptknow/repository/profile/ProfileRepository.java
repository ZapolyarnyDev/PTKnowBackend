package ptknow.repository.profile;

import ptknow.model.profile.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {
    @EntityGraph(attributePaths = "avatar")
    Optional<Profile> findByHandle(String handle);

    Optional<Profile> findByUserId(UUID userId);

    boolean existsByHandle(String handle);

    @Override
    @EntityGraph(attributePaths = "avatar")
    Page<Profile> findAll(Specification<Profile> spec, Pageable pageable);
}


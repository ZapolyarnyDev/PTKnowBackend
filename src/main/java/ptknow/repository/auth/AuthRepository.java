package ptknow.repository.auth;

import ptknow.model.auth.Auth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AuthRepository extends JpaRepository<Auth, UUID>, JpaSpecificationExecutor<Auth> {

    @Override
    @EntityGraph(attributePaths = "profile")
    Page<Auth> findAll(Specification<Auth> spec, Pageable pageable);

    Optional<Auth> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Auth> findAllByOrderByRegisteredAtDesc();
}


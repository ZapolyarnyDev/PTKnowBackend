package ptknow.repository.auth;

import ptknow.model.auth.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AuthRepository extends JpaRepository<Auth, UUID> {

    Optional<Auth> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Auth> findAllByOrderByRegisteredAtDesc();
}


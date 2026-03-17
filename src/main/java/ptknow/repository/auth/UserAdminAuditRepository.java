package ptknow.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptknow.model.auth.audit.UserAdminAudit;

@Repository
public interface UserAdminAuditRepository extends JpaRepository<UserAdminAudit, Long> {
}

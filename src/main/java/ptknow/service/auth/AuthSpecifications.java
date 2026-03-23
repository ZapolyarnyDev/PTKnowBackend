package ptknow.service.auth;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;

import java.util.Locale;

public final class AuthSpecifications {

    private AuthSpecifications() {
    }

    public static Specification<Auth> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }

            query.distinct(true);
            var profile = root.join("profile", JoinType.LEFT);
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(profile.get("fullName")), pattern),
                    cb.like(cb.lower(profile.get("handle")), pattern)
            );
        };
    }

    public static Specification<Auth> hasRole(Role role) {
        return (root, query, cb) ->
                role == null ? cb.conjunction() : cb.equal(root.get("role"), role);
    }

    public static Specification<Auth> hasStatus(UserStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
}

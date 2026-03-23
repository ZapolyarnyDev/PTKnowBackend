package ptknow.service.profile;

import org.springframework.data.jpa.domain.Specification;
import ptknow.model.profile.Profile;

import java.util.Locale;

public final class ProfileSpecifications {

    private ProfileSpecifications() {
    }

    public static Specification<Profile> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("handle")), pattern)
            );
        };
    }
}

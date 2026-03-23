package ptknow.mapper.course;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.mapper.ApiViewMapper;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.auth.UserStatus;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.course.CourseTag;
import ptknow.model.profile.Profile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseMapperTest {

    private final CourseMapper courseMapper = new CourseMapper(new ApiViewMapper());

    @Test
    void courseToDtoListShouldUseProvidedCounts() {
        Auth owner = auth("owner@test.local", "owner-handle", "Owner Name");
        Auth editor = auth("editor@test.local", "editor-handle", "Editor Name");

        Course course = Course.builder()
                .name("Java Backend")
                .description("desc")
                .handle("java-backend")
                .owner(owner)
                .state(CourseState.PUBLISHED)
                .maxUsersAmount(50)
                .build();
        ReflectionTestUtils.setField(course, "id", 42L);
        ReflectionTestUtils.setField(course, "courseTags", Set.of(new CourseTag("java")));
        ReflectionTestUtils.setField(course, "editors", Set.of(editor));

        var result = courseMapper.courseToDTOList(List.of(course), Map.of(42L, 12), Map.of(42L, 18));

        assertEquals(1, result.size());
        assertEquals(12, result.get(0).lessonsCount());
        assertEquals(18, result.get(0).studentsCount());
        assertEquals(2, result.get(0).teachersCount());
        assertEquals("owner-handle", result.get(0).owner().handle());
        assertEquals(1, result.get(0).editors().size());
    }

    private Auth auth(String email, String handle, String fullName) {
        Auth auth = Auth.builder()
                .email(email)
                .password("password")
                .role(Role.TEACHER)
                .build();
        ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(auth, "status", UserStatus.ACTIVE);

        Profile profile = Profile.builder()
                .fullName(fullName)
                .handle(handle)
                .user(auth)
                .build();
        ReflectionTestUtils.setField(auth, "profile", profile);
        return auth;
    }
}

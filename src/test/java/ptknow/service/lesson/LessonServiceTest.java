package ptknow.service.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.dto.lesson.CreateLessonDTO;
import ptknow.dto.lesson.UpdateLessonDTO;
import ptknow.dto.lesson.UpdateLessonStateDTO;
import ptknow.exception.lesson.LessonCannotBeCreatedException;
import ptknow.exception.lesson.LessonNotOwnedException;
import ptknow.exception.lesson.NotAllowedToSeeLessonInfo;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.course.Course;
import ptknow.model.course.CourseState;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.model.lesson.Lesson;
import ptknow.model.lesson.LessonState;
import ptknow.model.lesson.LessonType;
import ptknow.repository.lesson.LessonRepository;
import ptknow.service.course.CourseAccessService;
import ptknow.service.course.CourseService;
import ptknow.service.file.FileAttachmentService;
import ptknow.service.file.FileService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    LessonRepository lessonRepository;

    @Mock
    CourseService courseService;

    @Mock
    CourseAccessService accessService;

    @Mock
    FileService fileService;

    @Mock
    FileAttachmentService fileAttachmentService;

    @InjectMocks
    LessonService lessonService;

    @Test
    void createLessonShouldAllowCourseEditor() {
        Auth owner = auth(Role.TEACHER);
        Auth editor = auth(Role.TEACHER);
        Course course = course(1L, owner);
        course.addEditor(editor);
        CreateLessonDTO dto = createLessonDto();

        when(courseService.findCourseById(course.getId())).thenReturn(course);
        when(courseService.canEdit(course, editor)).thenReturn(true);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lesson lesson = lessonService.createLesson(course.getId(), editor, dto);

        assertSame(editor, lesson.getOwner());
        assertSame(course, lesson.getCourse());
        assertEquals(dto.name(), lesson.getName());
    }

    @Test
    void createLessonShouldRejectUserWithoutCourseEditPermission() {
        Auth owner = auth(Role.TEACHER);
        Auth outsider = auth(Role.TEACHER);
        Course course = course(1L, owner);

        when(courseService.findCourseById(course.getId())).thenReturn(course);
        when(courseService.canEdit(course, outsider)).thenReturn(false);

        assertThrows(LessonCannotBeCreatedException.class,
                () -> lessonService.createLesson(course.getId(), outsider, createLessonDto()));
    }

    @Test
    void seeByIdShouldReturnLessonWhenCourseIsVisible() {
        Lesson lesson = lesson(10L, course(1L, auth(Role.TEACHER)), auth(Role.TEACHER));

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(accessService.canSee(lesson.getCourse(), lesson.getOwner())).thenReturn(true);

        Lesson result = lessonService.seeById(lesson.getId(), lesson.getOwner());

        assertSame(lesson, result);
    }

    @Test
    void seeByIdShouldRejectWhenCourseIsNotVisible() {
        Auth viewer = auth(Role.GUEST);
        Lesson lesson = lesson(10L, course(1L, auth(Role.TEACHER)), auth(Role.TEACHER));

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(accessService.canSee(lesson.getCourse(), viewer)).thenReturn(false);

        assertThrows(NotAllowedToSeeLessonInfo.class, () -> lessonService.seeById(lesson.getId(), viewer));
    }

    @Test
    void updateByPatchShouldAllowOnlyLessonOwnerOrAdmin() {
        Auth owner = auth(Role.TEACHER);
        Lesson lesson = lesson(10L, course(1L, auth(Role.TEACHER)), owner);
        UpdateLessonDTO dto = new UpdateLessonDTO("new name", "new desc", null, null, LessonType.PRACTICE);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lesson result = lessonService.updateByPatch(lesson.getId(), owner, dto);

        assertEquals("new name", result.getName());
        assertEquals("new desc", result.getDescription());
        assertEquals(LessonType.PRACTICE, result.getType());
    }

    @Test
    void updateByPatchShouldRejectNonOwnerTeacher() {
        Auth owner = auth(Role.TEACHER);
        Auth editorTeacher = auth(Role.TEACHER);
        Lesson lesson = lesson(10L, course(1L, owner), owner);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));

        assertThrows(LessonNotOwnedException.class,
                () -> lessonService.updateByPatch(lesson.getId(), editorTeacher, new UpdateLessonDTO("n", null, null, null, null)));
    }

    @Test
    void deleteByIdShouldAllowCourseOwner() throws IOException {
        Auth courseOwner = auth(Role.TEACHER);
        Auth lessonOwner = auth(Role.TEACHER);
        Course course = course(1L, courseOwner);
        Lesson lesson = lesson(10L, course, lessonOwner);
        UUID fileId = UUID.randomUUID();

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(fileAttachmentService.deleteAllByResource(ResourceType.LESSON, lesson.getId().toString())).thenReturn(Set.of(fileId));
        when(fileAttachmentService.hasAttachments(fileId)).thenReturn(false);

        lessonService.deleteById(lesson.getId(), courseOwner);

        verify(lessonRepository).delete(lesson);
        verify(fileService).deleteFile(fileId);
    }

    @Test
    void deleteByIdShouldRejectOutsider() {
        Auth courseOwner = auth(Role.TEACHER);
        Auth lessonOwner = auth(Role.TEACHER);
        Auth outsider = auth(Role.TEACHER);
        Lesson lesson = lesson(10L, course(1L, courseOwner), lessonOwner);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));

        assertThrows(LessonNotOwnedException.class, () -> lessonService.deleteById(lesson.getId(), outsider));
        verify(lessonRepository, never()).delete(any());
    }

    @Test
    void updateStateShouldApplyNewStateForAdmin() {
        Auth owner = auth(Role.TEACHER);
        Auth admin = auth(Role.ADMIN);
        Lesson lesson = lesson(10L, course(1L, owner), owner);
        UpdateLessonStateDTO dto = new UpdateLessonStateDTO(LessonState.FINISHED);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lesson result = lessonService.updateState(lesson.getId(), admin, dto);

        assertEquals(LessonState.FINISHED, result.getState());
    }

    @Test
    void uploadMaterialShouldAttachAsEnrolledVisibility() throws IOException {
        Auth owner = auth(Role.TEACHER);
        Lesson lesson = lesson(10L, course(1L, auth(Role.TEACHER)), owner);
        org.springframework.web.multipart.MultipartFile multipartFile = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        File savedFile = File.builder()
                .id(UUID.randomUUID())
                .originalFilename("material.pdf")
                .contentType("application/pdf")
                .storagePath("uploads/test")
                .uploadedAt(Instant.now())
                .build();

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(fileService.saveFile(multipartFile)).thenReturn(savedFile);

        UUID result = lessonService.uploadMaterial(lesson.getId(), owner, multipartFile);

        assertEquals(savedFile.getId(), result);
        verify(fileAttachmentService).attach(
                eq(savedFile),
                eq(ResourceType.LESSON),
                eq(lesson.getId().toString()),
                eq(Purpose.MATERIAL),
                eq(FileVisibility.ENROLLED),
                eq(owner)
        );
    }

    @Test
    void canSeeShouldDelegateToCourseAccessServiceByCourseId() {
        Lesson lesson = lesson(10L, course(1L, auth(Role.TEACHER)), auth(Role.TEACHER));
        Auth viewer = auth(Role.STUDENT);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(accessService.canSee(lesson.getCourse().getId(), viewer)).thenReturn(true);

        boolean result = lessonService.canSee(lesson.getId(), viewer);

        assertEquals(true, result);
    }

    private CreateLessonDTO createLessonDto() {
        return new CreateLessonDTO(
                "Lesson 1",
                "Description",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                LessonType.LECTURE
        );
    }

    private Auth auth(Role role) {
        Auth auth = Auth.builder()
                .email(UUID.randomUUID() + "@test.local")
                .password("password")
                .role(role)
                .build();
        ReflectionTestUtils.setField(auth, "id", UUID.randomUUID());
        return auth;
    }

    private Course course(Long id, Auth owner) {
        Course course = Course.builder()
                .name("course-" + id)
                .description("desc")
                .handle("course-" + id)
                .owner(owner)
                .state(CourseState.DRAFT)
                .maxUsersAmount(10)
                .build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private Lesson lesson(Long id, Course course, Auth owner) {
        Lesson lesson = Lesson.builder()
                .name("lesson-" + id)
                .description("desc")
                .beginAt(Instant.now())
                .endsAt(Instant.now().plusSeconds(3600))
                .course(course)
                .lessonType(LessonType.LECTURE)
                .owner(owner)
                .build();
        ReflectionTestUtils.setField(lesson, "id", id);
        return lesson;
    }
}

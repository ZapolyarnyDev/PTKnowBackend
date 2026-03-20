package ptknow.service.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ptknow.exception.file.FileAttachmentNotFoundException;
import ptknow.model.auth.Auth;
import ptknow.model.auth.Role;
import ptknow.model.file.File;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.file.attachment.FileVisibility;
import ptknow.model.file.attachment.resource.Purpose;
import ptknow.model.file.attachment.resource.ResourceType;
import ptknow.repository.file.FileAttachmentRepository;
import ptknow.service.course.CourseService;
import ptknow.service.lesson.LessonService;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAccessServiceTest {

    static final AtomicLong ATTACHMENT_ID_SEQUENCE = new AtomicLong(1);

    @Mock
    FileAttachmentRepository attachmentRepository;

    @Mock
    LessonService lessonService;

    @Mock
    CourseService courseService;

    @InjectMocks
    FileAccessService fileAccessService;

    @Test
    void canReadShouldAllowAdminForAnyAttachment() {
        UUID fileId = UUID.randomUUID();
        Auth admin = auth(Role.ADMIN);
        FileAttachment attachment = attachment(fileId, auth(Role.TEACHER), ResourceType.COURSE, "1", FileVisibility.PRIVATE);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));

        assertTrue(fileAccessService.canRead(fileId, admin));
    }

    @Test
    void canReadShouldAllowPublicAttachment() {
        UUID fileId = UUID.randomUUID();
        Auth user = auth(Role.GUEST);
        FileAttachment attachment = attachment(fileId, auth(Role.TEACHER), ResourceType.PROFILE, UUID.randomUUID().toString(), FileVisibility.PUBLIC);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));

        assertTrue(fileAccessService.canRead(fileId, user));
    }

    @Test
    void canReadShouldAllowPrivateProfileForOwner() {
        UUID fileId = UUID.randomUUID();
        Auth owner = auth(Role.STUDENT);
        FileAttachment attachment = attachment(fileId, owner, ResourceType.PROFILE, UUID.randomUUID().toString(), FileVisibility.PRIVATE);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));

        assertTrue(fileAccessService.canRead(fileId, owner));
    }

    @Test
    void canReadShouldAllowPrivateCourseForCourseOwner() {
        UUID fileId = UUID.randomUUID();
        Auth viewer = auth(Role.TEACHER);
        FileAttachment attachment = attachment(fileId, auth(Role.TEACHER), ResourceType.COURSE, "42", FileVisibility.PRIVATE);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));
        when(courseService.isOwner(42L, viewer)).thenReturn(true);

        assertTrue(fileAccessService.canRead(fileId, viewer));
    }

    @Test
    void canReadShouldUseAccessServiceForEnrolledLessonAttachment() {
        UUID fileId = UUID.randomUUID();
        Auth viewer = auth(Role.STUDENT);
        FileAttachment attachment = attachment(fileId, auth(Role.TEACHER), ResourceType.LESSON, "12", FileVisibility.ENROLLED);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));
        when(lessonService.canSee(12L, viewer)).thenReturn(true);

        assertTrue(fileAccessService.canRead(fileId, viewer));
    }

    @Test
    void canReadShouldDenyWhenResourceIdIsInvalid() {
        UUID fileId = UUID.randomUUID();
        Auth viewer = auth(Role.STUDENT);
        FileAttachment attachment = attachment(fileId, auth(Role.TEACHER), ResourceType.COURSE, "bad-id", FileVisibility.ENROLLED);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(attachment));

        assertFalse(fileAccessService.canRead(fileId, viewer));
    }

    @Test
    void canDeleteShouldRequireOwnershipOfAllAttachments() {
        UUID fileId = UUID.randomUUID();
        Auth owner = auth(Role.TEACHER);
        Auth anotherOwner = auth(Role.TEACHER);
        FileAttachment first = attachment(fileId, owner, ResourceType.COURSE, "1", FileVisibility.PRIVATE);
        FileAttachment second = attachment(fileId, anotherOwner, ResourceType.LESSON, "2", FileVisibility.PRIVATE);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(first, second));

        assertFalse(fileAccessService.canDelete(fileId, owner));
    }

    @Test
    void canDeleteShouldAllowAdmin() {
        UUID fileId = UUID.randomUUID();

        assertTrue(fileAccessService.canDelete(fileId, auth(Role.ADMIN)));
    }

    @Test
    void canDeleteShouldAllowOwnerWhenAllAttachmentsBelongToUser() {
        UUID fileId = UUID.randomUUID();
        Auth owner = auth(Role.TEACHER);
        FileAttachment first = attachment(fileId, owner, ResourceType.COURSE, "1", FileVisibility.PRIVATE);
        FileAttachment second = attachment(fileId, owner, ResourceType.LESSON, "2", FileVisibility.PRIVATE);

        when(attachmentRepository.findAllByFile_Id(fileId)).thenReturn(Set.of(first, second));

        assertTrue(fileAccessService.canDelete(fileId, owner));
    }

    @Test
    void findByIdShouldThrowWhenAttachmentMissing() {
        when(attachmentRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(FileAttachmentNotFoundException.class, () -> fileAccessService.findById(1L));
    }

    @Test
    void getOwnerShouldReturnAttachmentOwner() {
        Auth owner = auth(Role.TEACHER);
        FileAttachment attachment = attachment(UUID.randomUUID(), owner, ResourceType.COURSE, "1", FileVisibility.PRIVATE);
        ReflectionTestUtils.setField(attachment, "id", 15L);

        when(attachmentRepository.findById(15L)).thenReturn(java.util.Optional.of(attachment));

        Auth result = fileAccessService.getOwner(15L);

        assertSame(owner, result);
        verify(attachmentRepository).findById(15L);
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

    private FileAttachment attachment(UUID fileId, Auth owner, ResourceType type, String resourceId, FileVisibility visibility) {
        File file = File.builder()
                .id(fileId)
                .originalFilename("name.txt")
                .contentType("text/plain")
                .storagePath("uploads/test")
                .uploadedAt(Instant.now())
                .build();

        FileAttachment attachment = FileAttachment.builder()
                .file(file)
                .owner(owner)
                .resourceType(type)
                .resourceId(resourceId)
                .purpose(Purpose.MATERIAL)
                .fileVisibility(visibility)
                .build();
        ReflectionTestUtils.setField(attachment, "id", ATTACHMENT_ID_SEQUENCE.getAndIncrement());
        return attachment;
    }
}

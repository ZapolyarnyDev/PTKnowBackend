package ptknow.model.auth;

import ptknow.model.course.Course;
import ptknow.model.enrollment.Enrollment;
import ptknow.model.file.attachment.FileAttachment;
import ptknow.model.lesson.Lesson;
import ptknow.model.profile.Profile;
import ptknow.exception.credentials.InvalidCredentialsException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "auth_data")
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Auth implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    UUID id;

    @Column(updatable = false, unique = true)
    @Getter
    String email;

    @Setter
    @Getter
    String password;

    @Column(nullable = false, updatable = false)
    @Getter
    Instant registeredAt;

    @Enumerated(EnumType.STRING)
    @Getter
    @Setter
    @Column(nullable = false)
    Role role;

    @Enumerated(EnumType.STRING)
    @Getter
    @Setter
    @Column(nullable = false)
    UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    @Getter
    AuthProvider authProvider;

    @Column(updatable = false, unique = true)
    @Getter
    String providerId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Getter
    private Profile profile;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    Set<Course> ownedCourses = new HashSet<>();

    @ManyToMany(mappedBy = "editors", fetch = FetchType.LAZY)
    Set<Course> editCourses = new HashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    Set<Lesson> ownedLessons = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    Set<FileAttachment> fileAttachments = new HashSet<>();

    @Builder
    public Auth(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.authProvider = AuthProvider.LOCAL;
    }

    @Builder(builderMethodName = "provideVK")
    public Auth(String providerId, Role role) {
        this.role = role;
        this.authProvider = AuthProvider.VK;
        this.providerId = providerId;
    }

    @PrePersist
    @PreUpdate
    public void validateFields() {
        if(registeredAt == null)
            registeredAt = Instant.now();

        if(role == null)
            role = Role.GUEST;
        if(status == null)
            status = UserStatus.ACTIVE;

        checkProvidingCredentials();
    }

    private void checkProvidingCredentials() {
        if(authProvider == AuthProvider.LOCAL){
            if(email == null || password == null)
                throw new InvalidCredentialsException("Local auth provider require email and password");
            if(email.isBlank() || password.isBlank())
                    throw new InvalidCredentialsException("Email and password cannot be empty in local auth provider");
        }
        else if(authProvider == AuthProvider.VK && providerId == null)
            throw new InvalidCredentialsException("VK auth provider require providerId");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(role.authorityName())
        );
    }

    @Override
    public String getUsername() {
        return email != null ? email : (authProvider.name().toLowerCase() + ":" + providerId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    public Set<FileAttachment> getFileAttachments() {
        return Collections.unmodifiableSet(fileAttachments);
    }

    public boolean addFileAttachment(FileAttachment attachment) {
        return fileAttachments.add(attachment);
    }

    public boolean removeFileAttachment(FileAttachment attachment) {
        return fileAttachments.remove(attachment);
    }

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public boolean addEnrollment(Enrollment enrollment) {
        return enrollments.add(enrollment);
    }

    public boolean removeEnrollment(Enrollment enrollment) {
        return enrollments.remove(enrollment);
    }

    public Set<Course> getOwnedCourses() {
        return Collections.unmodifiableSet(ownedCourses);
    }

    public boolean addOwnedLesson(Lesson lesson) {
       return ownedLessons.add(lesson);
    }

    public boolean removeOwnedLesson(Lesson lesson) {
       return ownedLessons.remove(lesson);
    }


    public Set<Lesson> getOwnedLessons() {
        return Collections.unmodifiableSet(ownedLessons);
    }

    public boolean addOwnedCourse(Course e) {
        return ownedCourses.add(e);
    }

    public boolean removeOwnedCourse(Course e) {
        return ownedCourses.remove(e);
    }

    public Set<Course> getEditCourses() {
        return Collections.unmodifiableSet(editCourses);
    }

    public boolean addEditCourse(Course e) {
        return editCourses.add(e);
    }

    public boolean removeEditCourse(Course e) {
        return editCourses.remove(e);
    }
}


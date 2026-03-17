package ptknow.model.course;

import ptknow.model.auth.Auth;
import ptknow.model.enrollment.Enrollment;
import ptknow.model.file.File;
import ptknow.model.lesson.Lesson;
import ptknow.exception.credentials.InvalidCredentialsException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@NoArgsConstructor
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_id_generator")
    @SequenceGenerator(name = "course_id_generator", sequenceName = "course_sequence", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Getter
    Long id;

    @Column(unique = true, nullable = false)
    @Getter
    String name;

    @Getter
    String description;

    @Getter
    @Column(unique = true, nullable = false)
    String handle;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "course_tags_mapping",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    Set<CourseTag> courseTags = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Lesson> lessons = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_editors_mapping",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "editor_id")
    )
    @Builder.Default
    Set<Auth> editors = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @Getter
    Auth owner;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Enrollment> enrollments = new HashSet<>();

    @Setter
    @Column(nullable = false)
    @Getter
    @Builder.Default
    int maxUsersAmount = 10;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    @Builder.Default
    CourseState state = CourseState.DRAFT;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_id")
    @Getter
    @Setter
    File preview;

    @PrePersist
    @PreUpdate
    public void checkCourseFields() {
        if(name == null || name.isBlank())
            throw new InvalidCredentialsException("Course name can't be null or blank");
        if (handle == null || handle.isBlank())
            throw new InvalidCredentialsException("Course handle can't be null or blank");
        if(maxUsersAmount <= 0)
            throw new InvalidCredentialsException("Course must be open to at least 1 person");
        if(state == null)
            state = CourseState.DRAFT;
    }

    public Set<Lesson> getLessons() {
        return Collections.unmodifiableSet(lessons);
    }

    public Set<CourseTag> getCourseTags() {
        return Collections.unmodifiableSet(courseTags);
    }

    public boolean hasEditor(Auth e) {
        return editors.contains(e);
    }

    public Set<Auth> getEditors() {
        return Collections.unmodifiableSet(editors);
    }

    public boolean addEditor(Auth e) {
        return e.addEditCourse(this) && editors.add(e);
    }

    public boolean removeEditor(Auth e) {
        return e.removeEditCourse(this) && editors.remove(e);
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
}


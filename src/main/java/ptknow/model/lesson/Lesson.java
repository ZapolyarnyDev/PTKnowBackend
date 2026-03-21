package ptknow.model.lesson;

import ptknow.model.auth.Auth;
import ptknow.model.course.Course;
import ptknow.exception.credentials.InvalidCredentialsException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "lesson")
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesson_id_generator")
    @SequenceGenerator(name = "lesson_id_generator", sequenceName = "lesson_sequence", allocationSize = 1)
    @EqualsAndHashCode.Include
    Long id;

    @Column(nullable = false)
    @Setter
    String name;

    @Setter
    String description;

    @Lob
    @Setter
    @Column(name = "content_md")
    String contentMd;

    @Column(nullable = false)
    @Setter
    Instant beginAt;

    @Column(nullable = false)
    @Setter
    Instant endsAt;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LessonState state = LessonState.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    LessonType type = LessonType.LECTURE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    Auth owner;

    @Builder
    public Lesson(String name, String description, String contentMd, Instant beginAt, Instant endsAt, Course course, LessonType lessonType, Auth owner) {
        this.name = name;
        this.description = description;
        this.contentMd = contentMd;
        this.beginAt = beginAt;
        this.endsAt = endsAt;
        this.course = course;
        this.type = lessonType;
        this.owner = owner;
    }

    @PrePersist
    @PreUpdate
    public void validateLesson() {
        if(name == null || name.isBlank())
            throw new InvalidCredentialsException("Lesson name can't be null or blank");
        if (beginAt == null || endsAt == null) {
            throw new InvalidCredentialsException("Lesson time can't be null");
        }
        if (endsAt.isBefore(beginAt)) {
            throw new InvalidCredentialsException("Lesson end time can't be before start time");
        }
        if (course == null) {
            throw new InvalidCredentialsException("Lesson must be linked to a course");
        }
    }
}


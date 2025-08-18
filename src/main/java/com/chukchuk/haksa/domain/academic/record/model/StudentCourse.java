package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "student_courses")
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @Column(name = "grade")
    private Grade grade;

    @Column(name = "points")
    private Integer points;

    @Column(name = "is_retake")
    private Boolean isRetake;

    @Column(name = "original_score")
    private Integer originalScore;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    private boolean isRetakeDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    public StudentCourse(
            Student student,
            CourseOffering offering,
            Grade grade,
            Integer points,
            Boolean isRetake,
            Integer originalScore,
            boolean isRetakeDeleted
    ) {
        this.student = student;
        this.offering = offering;
        this.grade = grade;
        this.points = points;
        this.isRetake = isRetake;
        this.originalScore = originalScore;
        this.isRetakeDeleted = isRetakeDeleted;
    }

    public void setStudent(Student student) {
        this.student = student;
    }


    public void setRetakeDeleted(Boolean aBoolean) {
        this.isRetakeDeleted = aBoolean;
    }
}

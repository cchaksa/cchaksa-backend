package com.chukchuk.haksa.domain.graduation.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "student_graduation_progress")
public class StudentGraduationProgress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "elective_courses_fulfilled")
    private Boolean electiveCoursesFulfilled;

    @Column(name = "credits_fulfilled")
    private Boolean creditsFulfilled;

    @Column(name = "language_cert_fulfilled")
    private Boolean languageCertFulfilled;

    @Column(name = "gpa_fulfilled")
    private Boolean gpaFulfilled;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "area_requirements_fulfilled")
    private Boolean areaRequirementsFulfilled;

    @Enumerated(EnumType.STRING)
    @Column(name = "graduation_status")
    private GraduationStatus graduationStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    private StudentGraduationProgress(Student student, Boolean languageCertFulfilled) {
        this.student = student;
        this.languageCertFulfilled = languageCertFulfilled;
    }

    public static StudentGraduationProgress createForLanguageCert(
            Student student,
            boolean languageCertFulfilled
    ) {
        return new StudentGraduationProgress(student, languageCertFulfilled);
    }

    public void updateLanguageCert(boolean languageCertFulfilled) {
        this.languageCertFulfilled = languageCertFulfilled;
    }
}

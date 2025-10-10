package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "semester_academic_records",
        indexes = {
                @Index(name = "idx_student_year_semester", columnList = "student_id, year, semester")
        }
)
@Access(AccessType.FIELD)
public class SemesterAcademicRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "year")
    private Integer year;

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "attempted_credits_gpa")
    private BigDecimal attemptedCreditsGpa;

    @Column(name = "semester_percentile")
    private BigDecimal semesterPercentile;

    @Column(name = "semester_gpa")
    private BigDecimal semesterGpa;

    @Column(name = "attempted_credits")
    private Integer attemptedCredits;

    @Column(name = "earned_credits")
    private Integer earnedCredits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    public SemesterAcademicRecord(
            Student student,
            Integer year,
            Integer semester,
            Integer attemptedCredits,
            Integer earnedCredits,
            BigDecimal semesterGpa,
            BigDecimal semesterPercentile,
            BigDecimal attemptedCreditsGpa,
            Integer classRank,
            Integer totalStudents
    ) {
        this.student = student;
        this.year = year;
        this.semester = semester;
        this.attemptedCredits = attemptedCredits;
        this.earnedCredits = earnedCredits;
        this.semesterGpa = semesterGpa;
        this.semesterPercentile = semesterPercentile;
        this.attemptedCreditsGpa = attemptedCreditsGpa;
        this.classRank = classRank;
        this.totalStudents = totalStudents;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Integer getSemester() {
        return this.semester;
    }

    private boolean compareBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    public boolean equalsContentOf(SemesterAcademicRecord other) {
        if (other == null) return false;

        return Objects.equals(this.year, other.year) &&
                Objects.equals(this.semester, other.semester) &&
                Objects.equals(this.totalStudents, other.totalStudents) &&
                Objects.equals(this.classRank, other.classRank) &&
                Objects.equals(this.attemptedCredits, other.attemptedCredits) &&
                Objects.equals(this.earnedCredits, other.earnedCredits) &&
                compareBigDecimal(this.semesterGpa, other.semesterGpa) &&
                compareBigDecimal(this.semesterPercentile, other.semesterPercentile) &&
                compareBigDecimal(this.attemptedCreditsGpa, other.attemptedCreditsGpa);
    }

    public void updateWith(SemesterAcademicRecord src) {
        this.year = src.year;
        this.semester = src.semester;
        this.totalStudents = src.totalStudents;
        this.classRank = src.classRank;
        this.attemptedCreditsGpa = src.attemptedCreditsGpa;
        this.semesterPercentile = src.semesterPercentile;
        this.semesterGpa = src.semesterGpa;
        this.attemptedCredits = src.attemptedCredits;
        this.earnedCredits = src.earnedCredits;
    }
}

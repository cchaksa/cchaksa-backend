package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.application.academic.AcademicSummary;
import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "student_academic_records")
public class StudentAcademicRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempted_credits_gpa")
    private BigDecimal attemptedCreditsGpa;

    @Column(name = "percentile")
    private BigDecimal percentile;

    @Column(name = "cumulative_gpa")
    private BigDecimal cumulativeGpa;

    @Column(name = "total_attempted_credits")
    private Integer totalAttemptedCredits;

    @Column(name = "total_earned_credits")
    private Integer totalEarnedCredits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true, nullable = false)
    private Student student;

    public StudentAcademicRecord(
            Student student,
            Integer totalAttemptedCredits,
            Integer totalEarnedCredits,
            BigDecimal cumulativeGpa,
            BigDecimal percentile
    ) {
        this.student = student;
        this.totalAttemptedCredits = totalAttemptedCredits;
        this.totalEarnedCredits = totalEarnedCredits;
        this.cumulativeGpa = cumulativeGpa;
        this.percentile = percentile;
    }

    public void updateWith(AcademicSummary summary) {
        this.totalAttemptedCredits = summary.getTotalAttemptedCredits();
        this.totalEarnedCredits = summary.getTotalEarnedCredits();
        this.cumulativeGpa = BigDecimal.valueOf(summary.getCumulativeGpa());
        this.percentile = BigDecimal.valueOf(summary.getPercentile());
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public boolean isSameAs(AcademicSummary summary) {
        return this.totalAttemptedCredits != null && this.totalAttemptedCredits.equals(summary.getTotalAttemptedCredits()) &&
                this.totalEarnedCredits != null && this.totalEarnedCredits.equals(summary.getTotalEarnedCredits()) &&
                this.cumulativeGpa != null && this.cumulativeGpa.compareTo(BigDecimal.valueOf(summary.getCumulativeGpa())) == 0 &&
                this.percentile != null && this.percentile.compareTo(BigDecimal.valueOf(summary.getPercentile())) == 0;
    }
}
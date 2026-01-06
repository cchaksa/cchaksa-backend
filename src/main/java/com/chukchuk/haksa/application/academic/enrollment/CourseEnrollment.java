package com.chukchuk.haksa.application.academic.enrollment;


import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CourseEnrollment {
    private UUID studentId;
    private Long offeringId;
    private Grade grade;
    private Integer points;
    private boolean isRetake;
    private Double originalScore;
    private boolean isRetakeDeleted;

    // 생성자
    public CourseEnrollment(UUID studentId, Long offeringId, Grade grade, Integer points, boolean isRetake, Double originalScore, boolean isRetakeDeleted) {
        this.studentId = studentId;
        this.offeringId = offeringId;
        this.grade = grade;
        this.points = points;
        this.isRetake = isRetake;
        this.originalScore = originalScore;
        this.isRetakeDeleted = isRetakeDeleted;
    }

    // getter 메서드들
    public UUID getStudentId() {
        return studentId;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public GradeType getGradeType() {
        return grade.getValue();
    }

    public Integer getPoints() {
        return points;
    }

    public boolean isRetake() {
        return isRetake;
    }

    public Double getOriginalScore() {
        return originalScore;
    }

    public boolean isCompleted() {
        return grade.isCompleted();
    }

    public boolean isPassed() {
        return grade.isPassingGrade();
    }

    public double getGradePoint() {
        return grade.getGradePoint();
    }

    // 재수강 관련 메서드
    public boolean isEligibleForRetake() {
        return !isRetake() && isCompleted() && (getGradeType() == GradeType.F || getGradePoint() <= 2.0); // C0 이하
    }
}

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

    public CourseEnrollment(UUID studentId, Long offeringId, Grade grade, Integer points, boolean isRetake, Double originalScore, boolean isRetakeDeleted) {
        this.studentId = studentId;
        this.offeringId = offeringId;
        this.grade = grade;
        this.points = points;
        this.isRetake = isRetake;
        this.originalScore = originalScore;
        this.isRetakeDeleted = isRetakeDeleted;
    }

    public GradeType getGradeType() {
        return grade.getValue();
    }
}

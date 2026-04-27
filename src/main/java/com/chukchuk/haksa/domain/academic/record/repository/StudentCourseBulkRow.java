package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.domain.student.model.GradeType;

import java.util.UUID;

public record StudentCourseBulkRow(
        UUID studentId,
        Long offeringId,
        GradeType gradeType,
        Integer points,
        boolean isRetake,
        Integer originalScore,
        boolean isRetakeDeleted
) {

    public static StudentCourseBulkRow from(CourseEnrollment enrollment) {
        Integer normalizedScore = enrollment.getOriginalScore() != null
                ? enrollment.getOriginalScore().intValue()
                : null;
        return new StudentCourseBulkRow(
                enrollment.getStudentId(),
                enrollment.getOfferingId(),
                enrollment.getGradeType(),
                enrollment.getPoints(),
                enrollment.isRetake(),
                normalizedScore,
                enrollment.isRetakeDeleted()
        );
    }
}

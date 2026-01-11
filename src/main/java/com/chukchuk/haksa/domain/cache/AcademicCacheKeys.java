package com.chukchuk.haksa.domain.cache;

import java.util.UUID;

public final class AcademicCacheKeys {

    private AcademicCacheKeys() {}

    public static String summary(UUID studentId) {
        return "student:" + studentId + ":summary";
    }

    public static String semesters(UUID studentId) {
        return "student:" + studentId + ":semesters";
    }

    public static String graduation(UUID studentId) {
        return "student:" + studentId + ":graduation";
    }

    public static String graduationRequirements(Long departmentId, Integer admissionYear) {
        return "graduation:requirements:" + departmentId + ":" + admissionYear;
    }

    public static String dualGraduationRequirements(
            Long primaryMajorId,
            Long secondaryMajorId,
            Integer admissionYear
    ) {
        return "graduation:dual-requirements:"
                + primaryMajorId + ":" + secondaryMajorId + ":" + admissionYear;
    }

    public static String semesterSummaries(UUID studentId) {
        return "student:" + studentId + ":semester-summaries";
    }

    public static String studentPrefix(UUID studentId) {
        return "student:" + studentId + ":";
    }
}
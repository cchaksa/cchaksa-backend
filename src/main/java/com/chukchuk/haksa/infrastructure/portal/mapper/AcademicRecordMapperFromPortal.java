package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.application.academic.AcademicRecord;
import com.chukchuk.haksa.application.academic.AcademicSummary;
import com.chukchuk.haksa.application.academic.SemesterGrade;
import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollments;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AcademicRecordMapperFromPortal {

    /**
     * PortalAcademicData를 AcademicRecord 도메인 모델로 변환한다.
     *
     * @param studentId - 학생의 ID
     * @param academicData - 포털에서 정제한 학업 정보 데이터
     * @return AcademicRecord 도메인 모델
     */
    public static AcademicRecord fromPortalAcademicData(UUID studentId, PortalAcademicData academicData) {
        if (studentId == null) {
            throw new CommonException(ErrorCode.STUDENT_ID_REQUIRED);
        }

        List<com.chukchuk.haksa.infrastructure.portal.model.SemesterGrade> semesters = academicData.grades().semesters();
        // PortalGradeSummary에 포함된 각 학기별 성적 데이터를 도메인 모델의 학기별 성적 객체로 변환
        List<SemesterGrade> semesterGrades = semesters.stream()
                .map(grade -> new SemesterGrade(
                        grade.year(),
                        grade.semester(),
                        // String을 int로 변환
                        parseToInt(grade.appliedCredits()),
                        // String을 int로 변환
                        parseToInt(grade.earnedCredits()),
                        // String을 double로 변환
                        parseToDouble(grade.semesterGpa()),
                        grade.score(),
                        null,
                        grade.ranking() != null ? grade.ranking().rank() : null,
                        grade.ranking() != null ? grade.ranking().total() : null
                ))
                .collect(Collectors.toList());

        // 전체 성적 요약 정보
        AcademicSummary summary = new AcademicSummary(
                academicData.summary().appliedCredits(),
                academicData.summary().totalCredits(),
                academicData.summary().gpa(),
                academicData.summary().score()
        );

        // 수강 이력은 포털 데이터에서 제공되지 않으므로 빈 배열 처리
        List<CourseEnrollment> enrollments = new ArrayList<>();  // 빈 리스트 생성
        // CourseEnrollments 객체 생성
        CourseEnrollments courseEnrollments = CourseEnrollments.create(enrollments);

        // AcademicRecord 도메인 모델 생성
        return new AcademicRecord(studentId, semesterGrades, summary, courseEnrollments);
    }

    private static int parseToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 적절한 기본값을 반환하거나 예외를 던짐
            return 0; // 또는 throw new InvalidDataException("Invalid integer value");
        }
    }

    private static double parseToDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // 적절한 기본값을 반환하거나 예외를 던짐
            return 0.0; // 또는 throw new InvalidDataException("Invalid double value");
        }
    }
}

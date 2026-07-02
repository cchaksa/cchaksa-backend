package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.application.academic.AcademicRecord;
import com.chukchuk.haksa.application.academic.AcademicSummary;
import com.chukchuk.haksa.application.academic.SemesterGrade;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicData;

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
                        parseToInt(grade.appliedCredits()),
                        parseToInt(grade.earnedCredits()),
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

        // AcademicRecord 도메인 모델 생성
        return new AcademicRecord(studentId, semesterGrades, summary);
    }

    private static int parseToInt(String value) {
        try {
            return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseToDouble(String value) {
        try {
            return (value == null || value.isBlank()) ? 0.0 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

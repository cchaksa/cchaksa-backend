// 사용자 학과에 적용되는 외국어 인증 기준을 전달하는 응답 DTO
package com.chukchuk.haksa.domain.graduation.dto;

import com.chukchuk.haksa.domain.graduation.model.LanguageCertMatchStatus;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertRequirement;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertTestType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "외국어 인증 기준 응답")
public record LanguageCertRequirementResponse(
        @Schema(description = "기준 조회에 사용된 학과 코드", example = "2000514")
        String departmentCode,
        @Schema(description = "기준 조회에 사용된 학과명", example = "컴퓨터SW")
        String departmentName,
        @Schema(description = "입학년도", example = "2021")
        Integer admissionYear,
        @Schema(description = "외국어 인증 정책 그룹 키", example = "ICT_OTHER", nullable = true)
        String policyGroupKey,
        @Schema(description = "외국어 인증 정책 그룹명", example = "ICT융합대학 그외학부", nullable = true)
        String policyGroupName,
        @Schema(description = "학과-정책 매핑 상태", implementation = LanguageCertMatchStatus.class)
        LanguageCertMatchStatus matchStatus,
        @Schema(description = "매핑 비고", example = "컴퓨터SW 21학번 이후 기준")
        String note,
        @Schema(description = "시험별 통과 기준")
        List<Requirement> requirements
) {

    public static LanguageCertRequirementResponse unmapped(
            String departmentCode,
            String departmentName,
            Integer admissionYear,
            String note
    ) {
        return new LanguageCertRequirementResponse(
                departmentCode,
                departmentName,
                admissionYear,
                null,
                null,
                LanguageCertMatchStatus.UNMAPPED,
                note,
                List.of()
        );
    }

    public record Requirement(
            @Schema(description = "시험 종류", implementation = LanguageCertTestType.class)
            LanguageCertTestType testType,
            @Schema(description = "최소 점수", example = "650", nullable = true)
            Integer minimumScore,
            @Schema(description = "최소 등급", example = "IM1", nullable = true)
            String minimumGrade,
            @Schema(description = "표시 문구", example = "TOEIC 650점 이상")
            String displayText,
            @Schema(description = "표시 순서", example = "1")
            Integer sortOrder
    ) {
        public static Requirement from(LanguageCertRequirement requirement) {
            return new Requirement(
                    requirement.getTestType(),
                    requirement.getMinimumScore(),
                    requirement.getMinimumGrade(),
                    requirement.getDisplayText(),
                    requirement.getSortOrder()
            );
        }
    }
}

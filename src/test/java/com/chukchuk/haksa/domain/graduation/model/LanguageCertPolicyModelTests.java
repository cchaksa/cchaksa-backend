// 외국어 인증 기준 정책 모델의 생성과 매핑 규칙을 검증하는 테스트
package com.chukchuk.haksa.domain.graduation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageCertPolicyModelTests {

    @Test
    @DisplayName("외국어 인증 기준 그룹을 생성한다")
    void createPolicyGroup() {
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                "데이터과학부를 제외한 ICT융합대학 기준"
        );

        assertThat(group.getGroupKey()).isEqualTo("ICT_OTHER");
        assertThat(group.getName()).isEqualTo("ICT융합대학 그외학부");
        assertThat(group.getDescription()).isEqualTo("데이터과학부를 제외한 ICT융합대학 기준");
    }

    @Test
    @DisplayName("점수형 시험 기준을 생성한다")
    void createScoreRequirement() {
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                null
        );

        LanguageCertRequirement requirement = LanguageCertRequirement.score(
                group,
                LanguageCertTestType.TOEIC,
                600,
                "TOEIC 600점 이상",
                1
        );

        assertThat(requirement.getPolicyGroup()).isEqualTo(group);
        assertThat(requirement.getTestType()).isEqualTo(LanguageCertTestType.TOEIC);
        assertThat(requirement.getMinimumScore()).isEqualTo(600);
        assertThat(requirement.getMinimumGrade()).isNull();
        assertThat(requirement.getDisplayText()).isEqualTo("TOEIC 600점 이상");
        assertThat(requirement.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("등급형 시험 기준을 생성한다")
    void createGradeRequirement() {
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                null
        );

        LanguageCertRequirement requirement = LanguageCertRequirement.grade(
                group,
                LanguageCertTestType.OPIC,
                "IM1",
                "OPIc IM1 이상",
                4
        );

        assertThat(requirement.getPolicyGroup()).isEqualTo(group);
        assertThat(requirement.getTestType()).isEqualTo(LanguageCertTestType.OPIC);
        assertThat(requirement.getMinimumScore()).isNull();
        assertThat(requirement.getMinimumGrade()).isEqualTo("IM1");
        assertThat(requirement.getDisplayText()).isEqualTo("OPIc IM1 이상");
        assertThat(requirement.getSortOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("학과 코드와 입학년도 구간이 일치하면 정책 매핑이 적용된다")
    void policyMappingAppliesByDepartmentCodeAndAdmissionYear() {
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                null
        );

        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.verified(
                        "2000514",
                        2021,
                        9999,
                        group,
                        "컴퓨터SW 21학번 이후 기준"
                );

        assertThat(mapping.appliesTo("2000514", 2021)).isTrue();
        assertThat(mapping.appliesTo("2000514", 2026)).isTrue();
        assertThat(mapping.appliesTo("2000514", 2020)).isFalse();
        assertThat(mapping.appliesTo("2000515", 2021)).isFalse();
        assertThat(mapping.getMatchStatus()).isEqualTo(LanguageCertMatchStatus.VERIFIED);
        assertThat(mapping.getPolicyGroup()).isEqualTo(group);
    }

    @Test
    @DisplayName("미매핑 학과 코드도 검토 상태로 기록할 수 있다")
    void createUnmappedPolicyMapping() {
        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.unmapped(
                        "2000763",
                        2021,
                        9999,
                        "자유전공학부는 기준표에 직접 행이 없음"
                );

        assertThat(mapping.getDepartmentCode()).isEqualTo("2000763");
        assertThat(mapping.getPolicyGroup()).isNull();
        assertThat(mapping.getMatchStatus()).isEqualTo(LanguageCertMatchStatus.UNMAPPED);
        assertThat(mapping.appliesTo("2000763", 2024)).isTrue();
    }
}

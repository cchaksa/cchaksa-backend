// 외국어 인증 정책 그룹별 시험 통과 기준 엔티티
package com.chukchuk.haksa.domain.graduation.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "language_cert_requirements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_language_cert_requirements_group_test",
                columnNames = {"policy_group_id", "test_type"}
        )
)
public class LanguageCertRequirement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_group_id", nullable = false)
    private LanguageCertPolicyGroup policyGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private LanguageCertTestType testType;

    @Column(name = "minimum_score")
    private Integer minimumScore;

    @Column(name = "minimum_grade")
    private String minimumGrade;

    @Column(name = "display_text", nullable = false)
    private String displayText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private LanguageCertRequirement(
            LanguageCertPolicyGroup policyGroup,
            LanguageCertTestType testType,
            Integer minimumScore,
            String minimumGrade,
            String displayText,
            Integer sortOrder
    ) {
        this.policyGroup = policyGroup;
        this.testType = testType;
        this.minimumScore = minimumScore;
        this.minimumGrade = minimumGrade;
        this.displayText = displayText;
        this.sortOrder = sortOrder;
    }

    public static LanguageCertRequirement score(
            LanguageCertPolicyGroup policyGroup,
            LanguageCertTestType testType,
            Integer minimumScore,
            String displayText,
            Integer sortOrder
    ) {
        return new LanguageCertRequirement(
                policyGroup,
                testType,
                minimumScore,
                null,
                displayText,
                sortOrder
        );
    }

    public static LanguageCertRequirement grade(
            LanguageCertPolicyGroup policyGroup,
            LanguageCertTestType testType,
            String minimumGrade,
            String displayText,
            Integer sortOrder
    ) {
        return new LanguageCertRequirement(
                policyGroup,
                testType,
                null,
                minimumGrade,
                displayText,
                sortOrder
        );
    }

    public static LanguageCertRequirement displayOnly(
            LanguageCertPolicyGroup policyGroup,
            LanguageCertTestType testType,
            String displayText,
            Integer sortOrder
    ) {
        return new LanguageCertRequirement(
                policyGroup,
                testType,
                null,
                null,
                displayText,
                sortOrder
        );
    }
}

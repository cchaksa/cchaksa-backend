// 학과 코드와 입학년도 구간을 외국어 인증 정책 그룹에 연결하는 엔티티
package com.chukchuk.haksa.domain.graduation.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "department_language_cert_policy_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dept_lang_cert_policy_year_range",
                columnNames = {"department_code", "admission_year_from", "admission_year_to"}
        ),
        indexes = {
                @Index(
                        name = "idx_dept_lang_cert_policy_lookup",
                        columnList = "department_code, admission_year_from, admission_year_to"
                )
        }
)
public class DepartmentLanguageCertPolicyMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Column(name = "department_code", nullable = false)
    private String departmentCode;

    @Column(name = "admission_year_from", nullable = false)
    private Integer admissionYearFrom;

    @Column(name = "admission_year_to", nullable = false)
    private Integer admissionYearTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_group_id")
    private LanguageCertPolicyGroup policyGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    private LanguageCertMatchStatus matchStatus;

    @Column(name = "note")
    private String note;

    private DepartmentLanguageCertPolicyMapping(
            String departmentCode,
            Integer admissionYearFrom,
            Integer admissionYearTo,
            LanguageCertPolicyGroup policyGroup,
            LanguageCertMatchStatus matchStatus,
            String note
    ) {
        this.departmentCode = departmentCode;
        this.admissionYearFrom = admissionYearFrom;
        this.admissionYearTo = admissionYearTo;
        this.policyGroup = policyGroup;
        this.matchStatus = matchStatus;
        this.note = note;
    }

    public static DepartmentLanguageCertPolicyMapping verified(
            String departmentCode,
            Integer admissionYearFrom,
            Integer admissionYearTo,
            LanguageCertPolicyGroup policyGroup,
            String note
    ) {
        return new DepartmentLanguageCertPolicyMapping(
                departmentCode,
                admissionYearFrom,
                admissionYearTo,
                policyGroup,
                LanguageCertMatchStatus.VERIFIED,
                note
        );
    }

    public static DepartmentLanguageCertPolicyMapping inferred(
            String departmentCode,
            Integer admissionYearFrom,
            Integer admissionYearTo,
            LanguageCertPolicyGroup policyGroup,
            String note
    ) {
        return new DepartmentLanguageCertPolicyMapping(
                departmentCode,
                admissionYearFrom,
                admissionYearTo,
                policyGroup,
                LanguageCertMatchStatus.INFERRED,
                note
        );
    }

    public static DepartmentLanguageCertPolicyMapping unmapped(
            String departmentCode,
            Integer admissionYearFrom,
            Integer admissionYearTo,
            String note
    ) {
        return new DepartmentLanguageCertPolicyMapping(
                departmentCode,
                admissionYearFrom,
                admissionYearTo,
                null,
                LanguageCertMatchStatus.UNMAPPED,
                note
        );
    }

    public boolean appliesTo(String departmentCode, int admissionYear) {
        return Objects.equals(this.departmentCode, departmentCode)
                && admissionYear >= admissionYearFrom
                && admissionYear <= admissionYearTo;
    }
}

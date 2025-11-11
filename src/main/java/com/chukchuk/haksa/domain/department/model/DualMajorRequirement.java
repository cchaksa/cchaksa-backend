package com.chukchuk.haksa.domain.department.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dual_major_requirements")
public class DualMajorRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "major_role", nullable = false)
    private MajorRole majorRole;

    @Column(name = "admission_year", nullable = false)
    private Integer admissionYear;

    @Column(name = "area_type", nullable = false)
    private String areaType;

    @Column(name = "required_credits", nullable = false)
    private Integer requiredCredits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}

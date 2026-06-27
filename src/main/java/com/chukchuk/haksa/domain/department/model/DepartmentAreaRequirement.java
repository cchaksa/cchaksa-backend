package com.chukchuk.haksa.domain.department.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_area_requirements")
public class DepartmentAreaRequirement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "total_elective_courses")
    private Integer totalElectiveCourses;

    @Column(name = "required_elective_courses")
    private Integer requiredElectiveCourses;

    @Column(name = "admission_year", nullable = false)
    private Integer admissionYear;

    @Column(name = "required_credits", nullable = false)
    private Integer requiredCredits;

    @Column(name = "description")
    private String description;

    @Column(name = "area_type", nullable = false)
    private String areaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public static DepartmentAreaRequirement create(
            Department department,
            Integer admissionYear,
            String areaType,
            Integer requiredCredits
    ) {
        DepartmentAreaRequirement requirement = new DepartmentAreaRequirement();
        requirement.department = department;
        requirement.admissionYear = admissionYear;
        requirement.areaType = areaType;
        requirement.requiredCredits = requiredCredits;
        return requirement;
    }
}

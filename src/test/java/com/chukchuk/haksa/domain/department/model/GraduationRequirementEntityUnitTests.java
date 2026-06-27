// 졸업요건 저장 엔티티 생성 메서드를 검증한다
package com.chukchuk.haksa.domain.department.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraduationRequirementEntityUnitTests {

    @Test
    @DisplayName("학과 졸업요건 row를 생성한다")
    void departmentAreaRequirement_create() {
        Department department = new Department("CSE", "컴퓨터공학");

        DepartmentAreaRequirement requirement =
                DepartmentAreaRequirement.create(department, 2026, "전핵", 24);

        assertThat(requirement.getDepartment()).isSameAs(department);
        assertThat(requirement.getAdmissionYear()).isEqualTo(2026);
        assertThat(requirement.getAreaType()).isEqualTo("전핵");
        assertThat(requirement.getRequiredCredits()).isEqualTo(24);
    }

    @Test
    @DisplayName("복수전공 졸업요건 row를 생성한다")
    void dualMajorRequirement_create() {
        Department department = new Department("CSE", "컴퓨터공학");

        DualMajorRequirement requirement =
                DualMajorRequirement.create(department, 2026, MajorRole.SECONDARY, "복핵", 27);

        assertThat(requirement.getDepartment()).isSameAs(department);
        assertThat(requirement.getAdmissionYear()).isEqualTo(2026);
        assertThat(requirement.getMajorRole()).isEqualTo(MajorRole.SECONDARY);
        assertThat(requirement.getAreaType()).isEqualTo("복핵");
        assertThat(requirement.getRequiredCredits()).isEqualTo(27);
    }
}

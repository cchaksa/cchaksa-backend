// PDF에서 추출한 졸업요건 템플릿을 표현한다
package com.chukchuk.haksa.domain.admin.dto;

import com.chukchuk.haksa.domain.department.model.MajorRole;

import java.util.List;

public record AdminGraduationRequirementTemplate(
        Integer admissionYear,
        String sourcePdf,
        Integer sourcePage,
        String collegeName,
        String departmentName,
        String majorName,
        List<String> matchNames,
        Integer graduationCredits,
        List<AreaRequirement> singleMajorRequirements,
        List<DualMajorRequirement> dualMajorRequirements
) {
    public record AreaRequirement(String areaType, Integer requiredCredits) {
    }

    public record DualMajorRequirement(MajorRole majorRole, String areaType, Integer requiredCredits) {
    }
}

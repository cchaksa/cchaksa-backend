// PDF 추출 졸업요건 리소스 조회를 검증한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminGraduationRequirementTemplate;
import com.chukchuk.haksa.domain.department.model.MajorRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminGraduationRequirementTemplateServiceUnitTests {

    private final AdminGraduationRequirementTemplateService service =
            new AdminGraduationRequirementTemplateService(new ObjectMapper());

    @Test
    @DisplayName("입학년도와 학과명으로 PDF 추출 템플릿을 찾는다")
    void findByAdmissionYearAndDepartmentName_returnsTemplate() {
        var template = service.findByAdmissionYearAndDepartmentName(2026, "컴퓨터 공학").orElseThrow();

        assertThat(template.matchNames()).contains("컴퓨터공학", "컴퓨터공학부");
        assertThat(template.singleMajorRequirements())
                .anySatisfy(requirement -> {
                    assertThat(requirement.areaType()).isEqualTo("전핵");
                    assertThat(requirement.requiredCredits()).isEqualTo(24);
                });
        assertThat(template.dualMajorRequirements())
                .anySatisfy(requirement -> {
                    assertThat(requirement.majorRole()).isEqualTo(MajorRole.SECONDARY);
                    assertThat(requirement.areaType()).isEqualTo("복핵");
                    assertThat(requirement.requiredCredits()).isEqualTo(27);
                });
    }

    @Test
    @DisplayName("동일한 요구 학점의 중복 템플릿은 하나의 템플릿으로 취급한다")
    void findByAdmissionYearAndDepartmentName_deduplicatesSameRequirementTemplates() {
        var template = service.findByAdmissionYearAndDepartmentName(2026, "한국언어문화").orElseThrow();

        assertThat(template.matchNames()).contains("한국언어문화");
        assertThat(template.graduationCredits()).isEqualTo(130);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("같은 입학년도 템플릿 리소스는 한 번만 읽는다")
    void findByAdmissionYearAndDepartmentName_cachesTemplatesByAdmissionYear() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(List.of(template("컴퓨터공학"), template("경영학")));
        AdminGraduationRequirementTemplateService cachedService =
                new AdminGraduationRequirementTemplateService(objectMapper);

        cachedService.findByAdmissionYearAndDepartmentName(2026, "컴퓨터공학");
        cachedService.findByAdmissionYearAndDepartmentName(2026, "경영학");

        verify(objectMapper, times(1)).readValue(any(InputStream.class), any(TypeReference.class));
    }

    private AdminGraduationRequirementTemplate template(String name) {
        return new AdminGraduationRequirementTemplate(
                2026,
                "2026_credits.pdf",
                1,
                "테스트대학",
                name,
                name,
                List.of(name),
                130,
                List.of(new AdminGraduationRequirementTemplate.AreaRequirement("전핵", 24)),
                List.of(new AdminGraduationRequirementTemplate.DualMajorRequirement(MajorRole.SECONDARY, "복핵", 27))
        );
    }
}

// PDF 추출 졸업요건 리소스를 조회한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminGraduationRequirementTemplate;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGraduationRequirementTemplateService {

    private static final String RESOURCE_PREFIX = "admin/graduation-requirements/";

    private final ObjectMapper objectMapper;

    public Optional<AdminGraduationRequirementTemplate> findByAdmissionYearAndDepartmentName(
            Integer admissionYear,
            String departmentName
    ) {
        String normalizedName = normalizeName(departmentName);
        if (admissionYear == null || normalizedName == null) {
            return Optional.empty();
        }
        List<AdminGraduationRequirementTemplate> matches = loadTemplates(admissionYear).stream()
                .filter(template -> template.matchNames() != null)
                .filter(template -> template.matchNames().stream()
                        .map(this::normalizeName)
                        .anyMatch(normalizedName::equals))
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        Set<String> signatures = matches.stream()
                .map(this::requirementSignature)
                .collect(Collectors.toSet());
        if (signatures.size() > 1) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        return Optional.of(matches.get(0));
    }

    private List<AdminGraduationRequirementTemplate> loadTemplates(Integer admissionYear) {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PREFIX + admissionYear + ".json");
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private String requirementSignature(AdminGraduationRequirementTemplate template) {
        return template.graduationCredits()
                + "|" + template.singleMajorRequirements()
                + "|" + template.dualMajorRequirements();
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\s+", "");
    }
}

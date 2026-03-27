package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalData(
        RawPortalStudentDTO student,
        List<RawPortalSemesterDTO> semesters,
        RawPortalGradeResponseDTO academicRecords
) {}

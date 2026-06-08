package com.chukchuk.haksa.infrastructure.portal.model;

public record PortalStudentInfo(
        String studentCode,
        String name,
        CodeName college,
        CodeName department,
        CodeName major,
        CodeName secondaryMajor,
        String status,
        AdmissionInfo admission,
        PortalAcademicInfo academic,
        Boolean languageCertFulfilled
) {}

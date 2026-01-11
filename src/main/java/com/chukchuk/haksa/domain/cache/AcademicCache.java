package com.chukchuk.haksa.domain.cache;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;

import java.util.List;
import java.util.UUID;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

public interface AcademicCache {

    void setAcademicSummary(UUID studentId, AcademicSummaryResponse summary);
    AcademicSummaryResponse getAcademicSummary(UUID studentId);

    void setSemesterList(UUID studentId, List<StudentSemesterDto.StudentSemesterInfoResponse> list);
    List<StudentSemesterDto.StudentSemesterInfoResponse> getSemesterList(UUID studentId);

    void setGraduationProgress(UUID studentId, GraduationProgressResponse progress);
    GraduationProgressResponse getGraduationProgress(UUID studentId);

    void setGraduationRequirements(Long departmentId, Integer admissionYear, List<AreaRequirementDto> requirements);
    List<AreaRequirementDto> getGraduationRequirements(Long departmentId, Integer admissionYear);

    void setDualMajorRequirements(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear, List<AreaRequirementDto> requirements);
    List<AreaRequirementDto> getDualMajorRequirements(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear);

    void setSemesterSummaries(UUID studentId, List<SemesterSummaryResponse> list);
    List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId);

    void deleteAllByStudentId(UUID studentId);
}
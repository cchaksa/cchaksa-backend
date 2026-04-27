package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalLinkJobQueryService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final StudentService studentService;

    @Transactional(readOnly = true)
    public PortalLinkDto.JobStatusResponse getJobStatus(UUID userId, String jobId) {
        ScrapeJob job = findOwnedJob(userId, jobId);

        return new PortalLinkDto.JobStatusResponse(
                job.getJobId(),
                job.getPortalType(),
                job.getStatus().name().toLowerCase(Locale.ROOT),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getRetryable(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getFinishedAt()
        );
    }

    @Transactional(readOnly = true)
    public PortalLinkDto.JobSummaryResponse getJobSummary(UUID userId, String jobId) {
        ScrapeJob job = findOwnedJob(userId, jobId);

        if (job.getStatus() == ScrapeJobStatus.FAILED) {
            throw new CommonException(ErrorCode.SCRAPE_JOB_FAILED_RESULT);
        }
        if (job.getStatus() != ScrapeJobStatus.SUCCEEDED) {
            throw new CommonException(ErrorCode.SCRAPE_JOB_NOT_COMPLETED);
        }

        Student student = studentService.getStudentByUserId(userId);
        if (student == null) {
            throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
        }

        PortalLinkDto.StudentInfoSummary studentInfo = mapStudentInfo(student);
        return new PortalLinkDto.JobSummaryResponse(
                job.getJobId(),
                job.getStatus().name().toLowerCase(Locale.ROOT),
                studentInfo,
                job.getFinishedAt()
        );
    }

    private ScrapeJob findOwnedJob(UUID userId, String jobId) {
        return scrapeJobRepository.findByJobIdAndUserId(jobId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));
    }

    private PortalLinkDto.StudentInfoSummary mapStudentInfo(Student student) {
        String name = defaultString(student.getName());
        String school = "수원대학교";
        String majorName = student.getMajor() != null
                ? defaultString(student.getMajor().getEstablishedDepartmentName())
                : student.getDepartment() != null
                ? defaultString(student.getDepartment().getEstablishedDepartmentName())
                : "";
        String studentCode = defaultString(student.getStudentCode());

        AcademicInfo academicInfo = student.getAcademicInfo();
        int gradeLevel = academicInfo != null && academicInfo.getGradeLevel() != null ? academicInfo.getGradeLevel() : 0;
        String status = academicInfo != null && academicInfo.getStatus() != null ? academicInfo.getStatus().name() : "";
        int completedSemesterType = resolveSemesterType(
                academicInfo != null ? academicInfo.getCompletedSemesters() : null
        );

        return new PortalLinkDto.StudentInfoSummary(
                name,
                school,
                majorName,
                studentCode,
                gradeLevel,
                status,
                completedSemesterType
        );
    }

    private static int resolveSemesterType(Integer completedSemesters) {
        int safe = completedSemesters != null ? completedSemesters : 0;
        return (safe % 2 == 0) ? 1 : 2;
    }

    private static String defaultString(String value) {
        return value != null ? value : "";
    }
}

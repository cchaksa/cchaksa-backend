package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLinkJobQueryServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private StudentService studentService;

    @Test
    @DisplayName("성공한 job은 학생 요약 정보를 반환한다")
    void getJobSummary_returnsStudentInfo() {
        UUID userId = UUID.randomUUID();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}"
        );
        Instant finishedAt = Instant.parse("2026-03-22T09:00:00Z");
        job.markSucceeded("{}", finishedAt);
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        Student student = mockStudent();
        when(studentService.getStudentByUserId(userId)).thenReturn(student);

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        PortalLinkDto.JobSummaryResponse response = service.getJobSummary(userId, job.getJobId());

        assertThat(response.job_id()).isEqualTo(job.getJobId());
        assertThat(response.studentInfo().majorName()).isEqualTo("소프트웨어학과");
        assertThat(response.studentInfo().completedSemesterType()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("succeeded");
        assertThat(response.finished_at()).isEqualTo(finishedAt);
    }

    @Test
    @DisplayName("미완료 job 요약 요청 시 예외를 던진다")
    void getJobSummary_throwsWhenJobNotCompleted() {
        UUID userId = UUID.randomUUID();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}"
        );
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        assertThatThrownBy(() -> service.getJobSummary(userId, job.getJobId()))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_NOT_COMPLETED.code()));
    }

    @Test
    @DisplayName("실패 job 요약 요청 시 실패 상태 예외를 던진다")
    void getJobSummary_throwsWhenJobFailed() {
        UUID userId = UUID.randomUUID();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}"
        );
        job.markFailed("INVALID_PAYLOAD", "missing", false, Instant.now());
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        assertThatThrownBy(() -> service.getJobSummary(userId, job.getJobId()))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_FAILED_RESULT.code()));
    }

    private static Student mockStudent() {
        Student student = mock(Student.class);
        when(student.getName()).thenReturn("홍길동");
        when(student.getStudentCode()).thenReturn("17019013");

        Department major = new Department("M01", "소프트웨어학과");
        when(student.getMajor()).thenReturn(major);

        AcademicInfo academicInfo = AcademicInfo.builder()
                .gradeLevel(2)
                .status(StudentStatus.재학)
                .completedSemesters(3)
                .build();
        when(student.getAcademicInfo()).thenReturn(academicInfo);

        return student;
    }
}

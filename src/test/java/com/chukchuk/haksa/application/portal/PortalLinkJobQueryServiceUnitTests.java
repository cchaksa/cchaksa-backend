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
    @DisplayName("미완료 job duration은 pending 상태와 null 소요 시간을 반환한다")
    void getJobDuration_returnsPendingWhenJobIsNotTerminal() {
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-06-04T10:00:00Z");
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}",
                startedAt
        );
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        PortalLinkDto.JobDurationResponse response = service.getJobDuration(userId, job.getJobId());

        assertThat(response.job_id()).isEqualTo(job.getJobId());
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.success()).isNull();
        assertThat(response.started_at()).isEqualTo(startedAt);
        assertThat(response.ended_at()).isNull();
        assertThat(response.elapsed_millis()).isNull();
        assertThat(response.elapsed_time()).isNull();
    }

    @Test
    @DisplayName("성공 job duration은 서버 종료 시각 기준 소요 시간을 반환한다")
    void getJobDuration_returnsSucceededElapsedTime() {
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-06-04T10:00:00Z");
        Instant workerFinishedAt = Instant.parse("2026-06-04T09:59:30Z");
        Instant serverEndedAt = Instant.parse("2026-06-04T10:00:12.345Z");
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}",
                startedAt
        );
        job.markSucceeded("{}", workerFinishedAt, serverEndedAt);
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        PortalLinkDto.JobDurationResponse response = service.getJobDuration(userId, job.getJobId());

        assertThat(response.status()).isEqualTo("succeeded");
        assertThat(response.success()).isTrue();
        assertThat(response.started_at()).isEqualTo(startedAt);
        assertThat(response.ended_at()).isEqualTo(serverEndedAt);
        assertThat(response.elapsed_millis()).isEqualTo(12_345L);
        assertThat(response.elapsed_time()).isEqualTo("12s 345ms");
    }

    @Test
    @DisplayName("실패 job duration은 failed 상태와 실패 소요 시간을 반환한다")
    void getJobDuration_returnsFailedElapsedTime() {
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-06-04T10:00:00Z");
        Instant workerFinishedAt = Instant.parse("2026-06-04T09:59:30Z");
        Instant serverEndedAt = Instant.parse("2026-06-04T10:00:03.120Z");
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "finger",
                "{\"username\":\"17019013\"}",
                startedAt
        );
        job.markFailed("INVALID_PAYLOAD", "missing", false, workerFinishedAt, serverEndedAt);
        when(scrapeJobRepository.findByJobIdAndUserId(eq(job.getJobId()), eq(userId))).thenReturn(Optional.of(job));

        PortalLinkJobQueryService service = new PortalLinkJobQueryService(scrapeJobRepository, studentService);

        PortalLinkDto.JobDurationResponse response = service.getJobDuration(userId, job.getJobId());

        assertThat(response.status()).isEqualTo("failed");
        assertThat(response.success()).isFalse();
        assertThat(response.ended_at()).isEqualTo(serverEndedAt);
        assertThat(response.elapsed_millis()).isEqualTo(3_120L);
        assertThat(response.elapsed_time()).isEqualTo("3s 120ms");
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

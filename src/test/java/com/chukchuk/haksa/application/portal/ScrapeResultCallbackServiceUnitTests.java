package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapeResultCallbackServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("잘못된 HMAC 서명은 거부한다")
    void handleCallback_rejectsInvalidSignature() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();

        assertThatThrownBy(() -> service.handleCallback(
                "{\"job_id\":\"job-1\",\"status\":\"failed\",\"error_code\":\"INVALID_PAYLOAD\",\"error_message\":\"bad\",\"retryable\":false,\"finished_at\":\"2026-03-14T10:01:00Z\"}",
                timestamp,
                "invalid-signature",
                null,
                null
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.INVALID_CALLBACK_SIGNATURE.code()));
    }

    @Test
    @DisplayName("성공 callback이면 LINK job을 성공 처리한다")
    void handleCallback_marksLinkJobSucceeded() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_payload":{
                    "student":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8},
                    "semesters":[{"semester":"2024-10","courses":[{"subjtCd":"C101","subjtNm":"자료구조","ltrPrfsNm":"김교수","estbDpmjNm":"컴퓨터학부","point":3,"cretGrdCd":"A+","refacYearSmr":"-","timtSmryCn":"월1-2","facDvnm":"전공","cltTerrNm":"0영역","cltTerrCd":"0","subjtEstbSmrCd":"10","subjtEstbYearSmr":"2024-10","diclNo":"01","gainPont":"95","cretDelCd":null,"cretDelNm":null}]}],
                    "academicRecords":{"listSmrCretSumTabYearSmr":[{"cretGainYear":"2024","cretSmrCd":"10","gainPoint":"18","applPoint":"18","gainAvmk":"4.2","gainTavgPont":"95","dpmjOrdp":"1/100"}],"selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}}
                  },
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.isCompleted()).isFalse();
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
        assertThat(job.getResultPayloadJson()).isNotBlank();
        ArgumentCaptor<PortalCallbackPostProcessCommand> captor = ArgumentCaptor.forClass(PortalCallbackPostProcessCommand.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PortalCallbackPostProcessCommand command = captor.getValue();
        assertThat(command.jobId()).isEqualTo(job.getJobId());
        assertThat(command.operationType()).isEqualTo(ScrapeJobOperationType.LINK);
        assertThat(command.finishedAt()).isEqualTo(Instant.parse("2026-03-14T10:01:00Z"));
    }

    @Test
    @DisplayName("완료된 job의 중복 callback은 무시한다")
    void handleCallback_ignoresDuplicateCallback() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                UUID.randomUUID(),
                "suwon",
                ScrapeJobOperationType.REFRESH,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        job.markFailed("INVALID_PAYLOAD", "bad", false, Instant.parse("2026-03-14T10:01:00Z"));

        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"failed",
                  "error_code":"INVALID_PAYLOAD",
                  "error_message":"bad",
                  "retryable":false,
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("실패 callback이면 FAILED 상태와 에러 정보를 저장한다")
    void handleCallback_marksJobFailed() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                UUID.randomUUID(),
                "suwon",
                ScrapeJobOperationType.REFRESH,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"failed",
                  "error_code":"PORTAL_AUTH_FAILED",
                  "error_message":"invalid credential",
                  "retryable":false,
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("PORTAL_AUTH_FAILED");
        assertThat(job.getRetryable()).isFalse();
    }

    @Test
    @DisplayName("성공 callback이면 REFRESH job을 재연동 흐름으로 처리한다")
    void handleCallback_marksRefreshJobSucceeded() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.REFRESH,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_payload":{
                    "student":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8},
                    "semesters":[{"semester":"2024-10","courses":[{"subjtCd":"C101","subjtNm":"자료구조","ltrPrfsNm":"김교수","estbDpmjNm":"컴퓨터학부","point":3,"cretGrdCd":"A+","refacYearSmr":"-","timtSmryCn":"월1-2","facDvnm":"전공","cltTerrNm":"0영역","cltTerrCd":"0","subjtEstbSmrCd":"10","subjtEstbYearSmr":"2024-10","diclNo":"01","gainPont":"95","cretDelCd":null,"cretDelNm":null}]}],
                    "academicRecords":{"listSmrCretSumTabYearSmr":[{"cretGainYear":"2024","cretSmrCd":"10","gainPoint":"18","applPoint":"18","gainAvmk":"4.2","gainTavgPont":"95","dpmjOrdp":"1/100"}],"selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}}
                  },
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.isCompleted()).isFalse();
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
        ArgumentCaptor<PortalCallbackPostProcessCommand> captor = ArgumentCaptor.forClass(PortalCallbackPostProcessCommand.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().operationType()).isEqualTo(ScrapeJobOperationType.REFRESH);
        assertThat(captor.getValue().finishedAt()).isEqualTo(Instant.parse("2026-03-14T10:01:00Z"));
    }

    @Test
    @DisplayName("성공 callback의 result_payload가 snake_case여도 성공 처리한다")
    void handleCallback_acceptsSnakeCaseResultPayload() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_payload":{
                    "student":{"sno":"17019013","stud_nm":"홍길동","univ_cd":"01","univ_nm":"수원대학교","dpmj_cd":"D1","dpmj_nm":"컴퓨터학부","mjor_cd":"M1","mjor_nm":"컴퓨터학과","the2_mjor_cd":null,"the2_mjor_nm":null,"scrg_stat_nm":"재학","ensc_year":"2021","ensc_smr_cd":"10","ensc_dvcd":"신입","stud_grde":4,"fac_smr_cnt":8},
                    "semesters":[{"semester":"2024-10","courses":[{"subjt_cd":"C101","subjt_nm":"자료구조","ltr_prfs_nm":"김교수","estb_dpmj_nm":"컴퓨터학부","point":3,"cret_grd_cd":"A+","refac_year_smr":"-","timt_smry_cn":"월1-2","fac_dvnm":"전공","clt_terr_nm":"0영역","clt_terr_cd":"0","subjt_estb_smr_cd":"10","subjt_estb_year_smr":"2024-10","dicl_no":"01","gain_pont":"95","cret_del_cd":null,"cret_del_nm":null}]}],
                    "academic_records":{"list_smr_cret_sum_tab_year_smr":[{"cret_gain_year":"2024","cret_smr_cd":"10","gain_point":"18","appl_point":"18","gain_avmk":"4.2","gain_tavg_pont":"95","dpmj_ordp":"1/100"}],"select_smr_cret_sum_tab_sj_total":{"gain_point":"120","appl_point":"130","gain_avmk":"3.8","gain_tavg_pont":"90"}}
                  },
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
        verify(eventPublisher).publishEvent(any(PortalCallbackPostProcessCommand.class));
    }

    @Test
    @DisplayName("성공 callback의 course에 extra field가 있어도 성공 처리한다")
    void handleCallback_ignoresUnknownCourseField() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        String timestamp = Instant.now().toString();
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_payload":{
                    "student":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8},
                    "semesters":[{"semester":"2024-10","courses":[{"subjtCd":"C101","subjtNm":"자료구조","ltrPrfsNm":"김교수","estbDpmjNm":"컴퓨터학부","point":3,"cretGrdCd":"A+","refacYearSmr":"-","timtSmryCn":"월1-2","facDvnm":"전공","cltTerrNm":"0영역","cltTerrCd":"0","subjtEstbYear":"2024","subjtEstbSmrCd":"10","subjtEstbYearSmr":"2024-10","diclNo":"01","gainPont":"95","cretDelCd":null,"cretDelNm":null}]}],
                    "academicRecords":{"listSmrCretSumTabYearSmr":[{"cretGainYear":"2024","cretSmrCd":"10","gainPoint":"18","applPoint":"18","gainAvmk":"4.2","gainTavgPont":"95","dpmjOrdp":"1/100"}],"selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}}
                  },
                  "finished_at":"2026-03-14T10:01:00Z"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
        verify(eventPublisher).publishEvent(any(PortalCallbackPostProcessCommand.class));
    }

    private ScrapeResultCallbackService createService() {
        return new ScrapeResultCallbackService(
                scrapeJobRepository,
                eventPublisher,
                new HmacSignatureVerifier("test-callback-secret", 300),
                meterRegistry
        );
    }

    private static String sign(String timestamp, String rawBody) {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("test-callback-secret", 300);
        byte[] signatureBytes = verifier.hmac(timestamp + "." + rawBody);
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}

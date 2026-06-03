package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.infrastructure.portal.client.ScrapeResultStoreClient;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapeResultCallbackServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private PortalCallbackPostProcessor portalCallbackPostProcessor;

    @Mock
    private ScrapeResultStoreClient resultStoreClient;

    @Mock
    private PortalSyncService portalSyncService;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(resultStoreClient.validateLocation(any()))
                .thenAnswer(invocation -> new ScrapeResultStoreClient.S3Location(
                        "bucket",
                        invocation.getArgument(0, String.class)
                ));
        lenient().when(resultStoreClient.isJobScopedLocation(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("잘못된 HMAC 서명은 거부한다")
    void handleCallback_rejectsInvalidSignature() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();

        assertThatThrownBy(() -> service.handleCallback(
                "{\"job_id\":\"job-1\",\"status\":\"failed\",\"error_code\":\"INVALID\"}",
                timestamp,
                "invalid-signature",
                null,
                null
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.INVALID_CALLBACK_SIGNATURE.code()));
    }

    @Test
    @DisplayName("HMAC 검증은 request parse보다 먼저 수행한다")
    void handleCallback_verifiesSignatureBeforeParsing() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();

        assertThatThrownBy(() -> service.handleCallback(
                "{invalid-json}",
                timestamp,
                "invalid-signature",
                null,
                null
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.INVALID_CALLBACK_SIGNATURE.code()));
    }

    @Test
    @DisplayName("성공 callback은 S3를 읽어 후처리를 동기 실행한다")
    void handleCallback_fetchesS3Synchronously() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        String timestamp = Instant.now().toString();
        ScrapeJob job = createJob(userId);

        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\",
                  \"result_s3_key\":\"callbacks/%s/result.json\",
                  \"finished_at\":\"2026-03-14T10:01:00Z\"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId())))
                .thenReturn("""
                        {
                          \"schema_version\":\"v1\",
                          \"student\":{\"sno\":\"17019013\",\"stud_nm\":\"홍길동\",\"univ_cd\":\"01\",\"univ_nm\":\"수원대학교\",\"stud_grde\":4},
                          \"semesters\":[{\"semester\":\"2024-10\",\"courses\":[{\"subjt_cd\":\"C101\",\"subjt_nm\":\"자료구조\"}]}],
                          \"academic_records\":{\"listSmrCretSumTabYearSmr\":[{\"cretGainYear\":\"2024\",\"gainPoint\":\"18\"}]}
                        }
                        """);
        doAnswer(invocation -> {
            assertThat(MDC.get("userId")).isEqualTo(userId.toString());
            assertThat(MDC.get("jobId")).isEqualTo(job.getJobId());
            assertThat(MDC.get("operationType")).isEqualTo("LINK");
            assertThat(MDC.get("workerRequestId")).isEqualTo("req-1");
            return null;
        }).when(portalCallbackPostProcessor)
                .process(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), "2", "req-1");

        verify(resultStoreClient).fetch("callbacks/%s/result.json".formatted(job.getJobId()));
        verify(portalCallbackPostProcessor).process(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                any(),
                any()
        );
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.POST_PROCESSING);
        assertThat(job.getResultS3Key()).isEqualTo("callbacks/%s/result.json".formatted(job.getJobId()));
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("jobId")).isNull();
    }

    @Test
    @DisplayName("checksum은 정규화 전 raw payload 기준으로 검증한다")
    void handleCallback_verifiesChecksumOnRawPayload() {
        ScrapeResultCallbackService service = createService();
        UUID userId = UUID.randomUUID();
        ScrapeJob job = createJob(userId);
        String timestamp = Instant.now().toString();
        String rawPayload = """
                {
                  "schema_version":"v1",
                  "student":{"sno":"17019013","stud_nm":"홍길동","univ_cd":"01","univ_nm":"수원대학교","stud_grde":4},
                  "semesters":[],
                  "academic_records":{"list_smry":[]}
                }
                """;
        String checksum = "sha256:" + sha256(rawPayload);
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_s3_key":"callbacks/%s/result.json",
                  "result_checksum":"%s"
                }
                """.formatted(job.getJobId(), job.getJobId(), checksum);

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId()))).thenReturn(rawPayload);

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, "req-1");

        verify(portalCallbackPostProcessor).process(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("이미 처리된 attempt면 중복으로 간주한다")
    void handleCallback_ignoresDuplicateAttempt() {
        ScrapeResultCallbackService service = createService();
        ScrapeJob job = createJob(UUID.randomUUID());
        job.recordCallbackAttempt(1, Instant.now());
        job.markFailed("FAILED_S3_READ", "fail", true, Instant.now());

        String timestamp = Instant.now().toString();
        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\",
                  \"result_s3_key\":\"callbacks/%s/result.json\"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), "1", "req-1");

        verify(resultStoreClient, never()).fetch(any());
        verify(portalCallbackPostProcessor, never()).process(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("실패 callback이면 FAILED 상태와 에러 정보를 저장한다")
    void handleCallback_marksJobFailed() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = createJob(UUID.randomUUID());
        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"failed\",
                  \"error_code\":\"PORTAL_AUTH_FAILED\",
                  \"error_message\":\"invalid credential\",
                  \"retryable\":false,
                  \"finished_at\":\"2026-03-14T10:01:00Z\"
                }
                """.formatted(job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null);

        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("PORTAL_AUTH_FAILED");
        assertThat(job.getRetryable()).isFalse();
        verify(resultStoreClient, never()).fetch(any());
    }

    @Test
    @DisplayName("result_s3_key 없이 성공 콜백이 오면 SCRAPE_INVALID_S3_KEY")
    void handleCallback_requiresS3Key() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = createJob(UUID.randomUUID());

        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\"
                }
                """.formatted(job.getJobId());

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_INVALID_S3_KEY.code()));
    }

    @Test
    @DisplayName("S3 key 형식 검증이 실패하면 SCRAPE_INVALID_S3_KEY를 반환한다")
    void handleCallback_rejectsInvalidS3KeyFormat() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = createJob(UUID.randomUUID());
        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\",
                  \"result_s3_key\":\"invalid/key.json\"
                }
                """.formatted(job.getJobId());

        when(resultStoreClient.validateLocation("invalid/key.json")).thenThrow(
                new ScrapeResultPayloadAccessException("SCRAPE_S3_FAILURE", "prefix mismatch", false)
        );

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_INVALID_S3_KEY.code()));
    }

    @Test
    @DisplayName("jobId가 path segment로 일치하지 않으면 SCRAPE_INVALID_S3_KEY를 반환한다")
    void handleCallback_rejectsKeyWithoutExactJobSegment() {
        ScrapeResultCallbackService service = createService();
        String timestamp = Instant.now().toString();
        ScrapeJob job = createJob(UUID.randomUUID());
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_s3_key":"callbacks/not-%s/result.json"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(resultStoreClient.validateLocation("callbacks/not-%s/result.json".formatted(job.getJobId())))
                .thenReturn(new ScrapeResultStoreClient.S3Location("bucket", "callbacks/not-%s/result.json".formatted(job.getJobId())));
        when(resultStoreClient.isJobScopedLocation(any(), eq(job.getJobId()))).thenReturn(false);

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, null))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_INVALID_S3_KEY.code()));
    }

    @Test
    @DisplayName("S3 읽기 실패 시 FAILED_S3_READ로 저장하고 SCRAPE_RESULT_S3_FAILED 반환")
    void handleCallback_marksS3Failure() {
        ScrapeResultCallbackService service = createService();
        ScrapeJob job = createJob(UUID.randomUUID());
        String timestamp = Instant.now().toString();

        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\",
                  \"result_s3_key\":\"callbacks/%s/result.json\"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId())))
                .thenThrow(new ScrapeResultPayloadAccessException("SCRAPE_S3_FAILURE", "missing", true));

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, "req-1"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_S3_FAILED.code()));
        assertThat(job.getErrorCode()).isEqualTo("FAILED_S3_READ");
    }

    @Test
    @DisplayName("후처리 실패는 SCRAPE_RESULT_POST_PROCESSING_FAILED로 전달된다")
    void handleCallback_propagatesPostProcessingFailure() {
        ScrapeResultCallbackService service = createService();
        ScrapeJob job = createJob(UUID.randomUUID());
        String timestamp = Instant.now().toString();
        String rawBody = """
                {
                  \"job_id\":\"%s\",
                  \"status\":\"succeeded\",
                  \"result_s3_key\":\"callbacks/%s/result.json\"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId())))
                .thenReturn("{\"schema_version\":\"v1\"}");
        doThrow(new CommonException(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED))
                .when(portalCallbackPostProcessor)
                .process(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, "req-1"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED.code()));
    }

    @Test
    @DisplayName("알 수 없는 flangPassGb 값은 schema invalid로 실패 확정한다")
    void handleCallback_marksUnknownLanguageCertAsSchemaFailure() {
        ScrapeResultCallbackService service = createServiceWithRealPostProcessor();
        ScrapeJob job = createJob(UUID.randomUUID());
        String timestamp = Instant.now().toString();
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_s3_key":"callbacks/%s/result.json"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId())))
                .thenReturn("""
                        {
                          "student_info":{"sno":"17019013","stud_nm":"홍길동","univ_cd":"01","univ_nm":"수원대학교","dpmj_cd":"D1","dpmj_nm":"컴퓨터학부","mjor_cd":"M1","mjor_nm":"컴퓨터학과","scrg_stat_nm":"재학","ensc_year":"2021","ensc_smr_cd":"10","ensc_dvcd":"신입","stud_grde":4,"fac_smr_cnt":8,"flang_pass_gb":"보류"},
                          "semesters":[],
                          "academic_records":{
                            "list_smr_cret_sum_tab_year_smr":[],
                            "select_smr_cret_sum_tab_sj_total":{"gain_point":"120","appl_point":"130","gain_avmk":"3.8","gain_tavg_pont":"90"}
                          }
                        }
                        """);

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, "req-1"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID.code()));

        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("FAILED_RESULT_SCHEMA");
        assertThat(job.getRetryable()).isFalse();
        verify(portalSyncService, never()).syncWithPortal(any(), any());
    }

    @Test
    @DisplayName("매핑 중 RuntimeException이 발생해도 schema invalid로 실패 확정한다")
    void handleCallback_marksMapperRuntimeExceptionAsSchemaFailure() {
        ScrapeResultCallbackService service = createServiceWithRealPostProcessor();
        ScrapeJob job = createJob(UUID.randomUUID());
        String timestamp = Instant.now().toString();
        String rawBody = """
                {
                  "job_id":"%s",
                  "status":"succeeded",
                  "result_s3_key":"callbacks/%s/result.json"
                }
                """.formatted(job.getJobId(), job.getJobId());

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.fetch("callbacks/%s/result.json".formatted(job.getJobId())))
                .thenReturn("""
                        {
                          "student_info":{"sno":"17019013","stud_nm":"홍길동","univ_cd":"01","univ_nm":"수원대학교","dpmj_cd":"D1","dpmj_nm":"컴퓨터학부","mjor_cd":"M1","mjor_nm":"컴퓨터학과","scrg_stat_nm":"재학","ensc_year":"2021","ensc_smr_cd":"10","ensc_dvcd":"신입","stud_grde":4,"fac_smr_cnt":8,"flang_pass_gb":"통과"},
                          "semesters":[{"semester":"2024","courses":[]}],
                          "academic_records":{
                            "list_smr_cret_sum_tab_year_smr":[],
                            "select_smr_cret_sum_tab_sj_total":{"gain_point":"120","appl_point":"130","gain_avmk":"3.8","gain_tavg_pont":"90"}
                          }
                        }
                        """);

        assertThatThrownBy(() -> service.handleCallback(rawBody, timestamp, sign(timestamp, rawBody), null, "req-1"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID.code()));

        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("FAILED_RESULT_SCHEMA");
        assertThat(job.getRetryable()).isFalse();
        verify(portalSyncService, never()).syncWithPortal(any(), any());
    }

    private ScrapeResultCallbackService createService() {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("secret", 300);
        ScrapeResultCallbackTxService txService = new ScrapeResultCallbackTxService(
                scrapeJobRepository,
                portalSyncService,
                meterRegistry
        );
        return new ScrapeResultCallbackService(
                portalCallbackPostProcessor,
                txService,
                resultStoreClient,
                verifier,
                meterRegistry,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private ScrapeResultCallbackService createServiceWithRealPostProcessor() {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("secret", 300);
        ScrapeResultCallbackTxService txService = new ScrapeResultCallbackTxService(
                scrapeJobRepository,
                portalSyncService,
                meterRegistry
        );
        PortalCallbackPostProcessor realPostProcessor = new PortalCallbackPostProcessor(
                new ObjectMapper().findAndRegisterModules(),
                meterRegistry,
                txService
        );
        return new ScrapeResultCallbackService(
                realPostProcessor,
                txService,
                resultStoreClient,
                verifier,
                meterRegistry,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private ScrapeJob createJob(UUID userId) {
        return ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{\"username\":\"170\",\"password\":\"pw\"}"
        );
    }

    private String sign(String timestamp, String rawBody) {
        String data = timestamp + "." + rawBody;
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("secret", 300);
        return Base64.getEncoder().encodeToString(verifier.hmac(data));
    }

    private String sha256(String rawBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawBody.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}

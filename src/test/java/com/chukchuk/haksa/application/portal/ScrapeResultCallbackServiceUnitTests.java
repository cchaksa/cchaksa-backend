package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.infrastructure.portal.client.ScrapeResultResultStoreClient;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private ScrapeResultResultStoreClient resultStoreClient;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(resultStoreClient.validateLocation(any()))
                .thenAnswer(invocation -> new ScrapeResultResultStoreClient.S3Location(
                        "bucket",
                        invocation.getArgument(0, String.class)
                ));
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
        assertThat(job.getResultPayloadJson()).isNotBlank();
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

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

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

        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(resultStoreClient.validateLocation("invalid/key.json")).thenThrow(
                new ScrapeResultPayloadAccessException("SCRAPE_S3_FAILURE", "prefix mismatch", false)
        );

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

    private ScrapeResultCallbackService createService() {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("secret", 300);
        return new ScrapeResultCallbackService(
                scrapeJobRepository,
                portalCallbackPostProcessor,
                resultStoreClient,
                verifier,
                meterRegistry
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
}

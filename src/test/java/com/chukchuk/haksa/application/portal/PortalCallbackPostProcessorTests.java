package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalCallbackPostProcessorTests {

    private static final String PAYLOAD_JSON = """
            {
              "student":{"sno":"17019013","studNm":"홍길동","univCd":"01","univNm":"수원대학교","dpmjCd":"D1","dpmjNm":"컴퓨터학부","mjorCd":"M1","mjorNm":"컴퓨터학과","the2MjorCd":null,"the2MjorNm":null,"scrgStatNm":"재학","enscYear":"2021","enscSmrCd":"10","enscDvcd":"신입","studGrde":4,"facSmrCnt":8},
              "semesters":[{"semester":"2024-10","courses":[{"subjtCd":"C101","subjtNm":"자료구조","ltrPrfsNm":"김교수","estbDpmjNm":"컴퓨터학부","point":3,"cretGrdCd":"A+","refacYearSmr":"-","timtSmryCn":"월1-2","facDvnm":"전공","cltTerrNm":"0영역","cltTerrCd":"0","subjtEstbSmrCd":"10","subjtEstbYearSmr":"2024-10","diclNo":"01","gainPont":"95","cretDelCd":null,"cretDelNm":null}]}],
              "academicRecords":{
                "listSmrCretSumTabYearSmr":[{"cretGainYear":"2024","cretSmrCd":"10","gainPoint":"18","applPoint":"18","gainAvmk":"4.2","gainTavgPont":"95","dpmjOrdp":"1/100"}],
                "selectSmrCretSumTabSjTotal":{"gainPoint":"120","applPoint":"130","gainAvmk":"3.8","gainTavgPont":"90"}
              }
            }
            """;

    @Mock
    private PortalSyncService portalSyncService;

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    private SimpleMeterRegistry meterRegistry;
    private PortalCallbackPostProcessor processor;
    private ScrapeResultCallbackTxService txService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        txService = new ScrapeResultCallbackTxService(scrapeJobRepository, portalSyncService, meterRegistry);
        processor = new PortalCallbackPostProcessor(
                new ObjectMapper().findAndRegisterModules(),
                meterRegistry,
                txService
        );
    }

    @Test
    @DisplayName("LINK 후처리가 성공하면 portal sync가 호출되고 성공 메트릭이 증가한다")
    void handle_linkSuccess() {
        ScrapeJob job = newJob(ScrapeJobOperationType.LINK);
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        Instant finishedAt = Instant.parse("2026-04-08T00:00:00Z");
        processor.process(
                job.getJobId(),
                job.getUserId(),
                ScrapeJobOperationType.LINK,
                PAYLOAD_JSON,
                finishedAt,
                1.0,
                1,
                "",
                "payload-hash"
        );

        verify(portalSyncService).syncWithPortal(eq(job.getUserId()), any());
        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.success").count()).isEqualTo(1.0);
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("PORTAL refresh 실패는 실패 메트릭에 reason=portal_conn_fail로 기록된다")
    void handle_portalFailure_recordsPortalReason() {
        ScrapeJob job = newJob(ScrapeJobOperationType.REFRESH);
        job.markPostProcessing("callbacks/" + job.getJobId() + "/result.json", null, null, 1, Instant.now());
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        Instant finishedAt = Instant.parse("2026-04-08T00:00:00Z");
        doThrow(new PortalScrapeException(ErrorCode.SCRAPING_FAILED)).when(portalSyncService).refreshFromPortal(eq(job.getUserId()), any());

        assertThatThrownBy(() -> processor.process(
                job.getJobId(),
                job.getUserId(),
                ScrapeJobOperationType.REFRESH,
                PAYLOAD_JSON,
                finishedAt,
                1.0,
                1,
                "",
                "payload-hash"
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED.code()));

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "portal_conn_fail").count()).isEqualTo(1.0);
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.FAILED);
    }

    @Test
    @DisplayName("EntityNotFoundException은 reason=user_missing으로 기록된다")
    void handle_userMissing_recordsReason() {
        ScrapeJob job = newJob(ScrapeJobOperationType.LINK);
        job.markPostProcessing("callbacks/" + job.getJobId() + "/result.json", null, null, 1, Instant.now());
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        Instant finishedAt = Instant.parse("2026-04-08T00:00:00Z");
        doThrow(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND)).when(portalSyncService).syncWithPortal(eq(job.getUserId()), any());

        assertThatThrownBy(() -> processor.process(
                job.getJobId(),
                job.getUserId(),
                ScrapeJobOperationType.LINK,
                PAYLOAD_JSON,
                finishedAt,
                1.0,
                1,
                "",
                "payload-hash"
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED.code()));

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "user_missing").count()).isEqualTo(1.0);
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.FAILED);
    }

    @Test
    @DisplayName("Json 파싱 실패는 portal sync를 호출하지 않고 invalid_payload 메트릭을 증가시킨다")
    void handle_invalidPayload_recordsFailure() {
        ScrapeJob job = newJob(ScrapeJobOperationType.LINK);
        job.markPostProcessing("callbacks/" + job.getJobId() + "/result.json", null, null, 1, Instant.now());
        Instant finishedAt = Instant.parse("2026-04-08T00:00:00Z");
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> processor.process(
            job.getJobId(),
            job.getUserId(),
            ScrapeJobOperationType.LINK,
            "{invalid-json}",
            finishedAt,
            1.0,
            1,
            "",
            "invalid"
        )).isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID.code()));

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload").count()).isEqualTo(1.0);
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.FAILED);
    }

    private static ScrapeJob newJob(ScrapeJobOperationType operationType) {
        return ScrapeJob.createQueued(
                UUID.randomUUID(),
                "suwon",
                operationType,
                "idem-1",
                "fingerprint",
                "{}"
        );
    }
}

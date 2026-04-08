package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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

    private SimpleMeterRegistry meterRegistry;
    private PortalCallbackPostProcessor processor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        processor = new PortalCallbackPostProcessor(portalSyncService, new ObjectMapper().findAndRegisterModules(), meterRegistry);
    }

    @Test
    @DisplayName("LINK 후처리가 성공하면 portal sync가 호출되고 성공 메트릭이 증가한다")
    void handle_linkSuccess() {
        PortalCallbackPostProcessCommand command = command(ScrapeJobOperationType.LINK);

        processor.handle(command);

        verify(portalSyncService).syncWithPortal(eq(command.userId()), any());
        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.success").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("PORTAL refresh 실패는 실패 메트릭에 reason=portal_conn_fail로 기록된다")
    void handle_portalFailure_recordsPortalReason() {
        PortalCallbackPostProcessCommand command = command(ScrapeJobOperationType.REFRESH);
        doThrow(new PortalScrapeException(ErrorCode.SCRAPING_FAILED)).when(portalSyncService).refreshFromPortal(eq(command.userId()), any());

        processor.handle(command);

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "portal_conn_fail").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("EntityNotFoundException은 reason=user_missing으로 기록된다")
    void handle_userMissing_recordsReason() {
        PortalCallbackPostProcessCommand command = command(ScrapeJobOperationType.LINK);
        doThrow(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND)).when(portalSyncService).syncWithPortal(eq(command.userId()), any());

        processor.handle(command);

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "user_missing").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Json 파싱 실패는 portal sync를 호출하지 않고 invalid_payload 메트릭을 증가시킨다")
    void handle_invalidPayload_recordsFailure() {
        PortalCallbackPostProcessCommand command = new PortalCallbackPostProcessCommand(UUID.randomUUID().toString(), UUID.randomUUID(), ScrapeJobOperationType.LINK, "{invalid-json}");

        processor.handle(command);

        assertThat(meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload").count()).isEqualTo(1.0);
    }

    private static PortalCallbackPostProcessCommand command(ScrapeJobOperationType operationType) {
        return new PortalCallbackPostProcessCommand(UUID.randomUUID().toString(), UUID.randomUUID(), operationType, PAYLOAD_JSON);
    }
}

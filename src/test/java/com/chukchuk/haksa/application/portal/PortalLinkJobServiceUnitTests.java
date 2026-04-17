package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLinkJobServiceUnitTests {

    @Mock
    private PortalLinkJobTxService portalLinkJobTxService;

    @Mock
    private ScrapeJobOutboxDispatcher scrapeJobOutboxDispatcher;

    @Mock
    private UserService userService;

    @Test
    @DisplayName("새 요청은 job/outbox 저장 후 같은 요청에서 동기 publish 한다")
    void acceptLinkJob_dispatchesSynchronously() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(portalLinkJobTxService, scrapeJobOutboxDispatcher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");
        PortalLinkJobTxService.PreparedJob preparedJob = new PortalLinkJobTxService.PreparedJob("job-1", "outbox-1", false, true);

        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(portalLinkJobTxService.createOrLoadJob(eq(userId), eq("idem-1"), eq("suwon"), eq(ScrapeJobOperationType.LINK), any(), any(), eq("17019013"), eq("pw"), any()))
                .thenReturn(preparedJob);
        when(portalLinkJobTxService.loadDispatchSnapshot("outbox-1"))
                .thenReturn(new PortalLinkJobTxService.DispatchSnapshot(
                        "job-1",
                        "outbox-1",
                        ScrapeJobStatus.RUNNING,
                        ScrapeJobOutboxStatus.SENT,
                        "msg-1",
                        null
                ));

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.job_id()).isEqualTo("job-1");
        assertThat(response.status()).isEqualTo("accepted");
        verify(scrapeJobOutboxDispatcher).dispatchOnce("outbox-1");
    }

    @Test
    @DisplayName("이미 RUNNING 이상인 동일 요청은 기존 job을 재사용하고 publish 하지 않는다")
    void acceptLinkJob_reusesExistingJobWithoutDispatch() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(portalLinkJobTxService, scrapeJobOutboxDispatcher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");
        PortalLinkJobTxService.PreparedJob preparedJob = new PortalLinkJobTxService.PreparedJob("job-1", "outbox-1", true, false);

        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(portalLinkJobTxService.createOrLoadJob(eq(userId), eq("idem-1"), eq("suwon"), eq(ScrapeJobOperationType.LINK), any(), any(), eq("17019013"), eq("pw"), any()))
                .thenReturn(preparedJob);

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.job_id()).isEqualTo("job-1");
        verify(scrapeJobOutboxDispatcher, never()).dispatchOnce(any());
    }

    @Test
    @DisplayName("동기 publish 후 SENT/RUNNING이 아니면 enqueue 실패로 처리한다")
    void acceptLinkJob_throwsWhenDispatchStateIsNotSent() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(portalLinkJobTxService, scrapeJobOutboxDispatcher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");
        PortalLinkJobTxService.PreparedJob preparedJob = new PortalLinkJobTxService.PreparedJob("job-1", "outbox-1", false, true);

        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(portalLinkJobTxService.createOrLoadJob(eq(userId), eq("idem-1"), eq("suwon"), eq(ScrapeJobOperationType.LINK), any(), any(), eq("17019013"), eq("pw"), any()))
                .thenReturn(preparedJob);
        when(portalLinkJobTxService.loadDispatchSnapshot("outbox-1"))
                .thenReturn(new PortalLinkJobTxService.DispatchSnapshot(
                        "job-1",
                        "outbox-1",
                        ScrapeJobStatus.QUEUED,
                        ScrapeJobOutboxStatus.RETRYABLE_FAILED,
                        null,
                        "temporary failure"
                ));

        assertThatThrownBy(() -> service.acceptJob(userId, "idem-1", request))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED.code()));
    }

    @Test
    @DisplayName("동시성으로 최초 저장이 충돌하면 기존 job을 다시 조회해 같은 요청 흐름에서 publish 한다")
    void acceptLinkJob_resolvesConcurrentDuplicateAndDispatches() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(portalLinkJobTxService, scrapeJobOutboxDispatcher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");
        PortalLinkJobTxService.PreparedJob preparedJob = new PortalLinkJobTxService.PreparedJob("job-1", "outbox-1", true, true);

        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(portalLinkJobTxService.createOrLoadJob(eq(userId), eq("idem-1"), eq("suwon"), eq(ScrapeJobOperationType.LINK), any(), any(), eq("17019013"), eq("pw"), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(portalLinkJobTxService.loadExistingJob(eq(userId), eq("idem-1"), any())).thenReturn(preparedJob);
        when(portalLinkJobTxService.loadDispatchSnapshot("outbox-1"))
                .thenReturn(new PortalLinkJobTxService.DispatchSnapshot(
                        "job-1",
                        "outbox-1",
                        ScrapeJobStatus.RUNNING,
                        ScrapeJobOutboxStatus.SENT,
                        "msg-1",
                        null
                ));

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.job_id()).isEqualTo("job-1");
        verify(scrapeJobOutboxDispatcher).dispatchOnce("outbox-1");
    }

    private static User disconnectedUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("disconnected@example.com")
                .profileNickname("tester")
                .build();
    }
}

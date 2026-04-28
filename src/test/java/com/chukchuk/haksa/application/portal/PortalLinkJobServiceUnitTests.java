package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLinkJobServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ScrapeJobOutboxRepository scrapeJobOutboxRepository;

    @Mock
    private UserService userService;

    @Mock
    private ScrapeJobOutboxAfterCommitExecutor scrapeJobOutboxAfterCommitExecutor;

    @Test
    @DisplayName("새 idempotency key 요청이면 job과 outbox를 함께 저장한다")
    void acceptLinkJob_createsJobAndOutbox() throws Exception {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobOutboxRepository, userService, scrapeJobOutboxAfterCommitExecutor);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scrapeJobOutboxRepository.save(any(ScrapeJobOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(scrapeJobOutboxAfterCommitExecutor).dispatchAsync(any(), any());

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(response.job_id()).isNotBlank();
        ArgumentCaptor<ScrapeJobOutbox> captor = ArgumentCaptor.forClass(ScrapeJobOutbox.class);
        verify(scrapeJobOutboxRepository).save(captor.capture());
        assertThat(captor.getValue().getJobId()).isEqualTo(response.job_id());
        assertThat(captor.getValue().getPayloadJson()).contains("\"job_id\"");
        JsonNode payload = new ObjectMapper().readTree(captor.getValue().getPayloadJson());
        assertThat(payload.path("requested_at").isTextual()).isTrue();
        assertThat(payload.path("requested_at").asText()).contains("T");
        verify(scrapeJobOutboxAfterCommitExecutor).dispatchAsync(any(ScrapeJob.class), any(ScrapeJobOutbox.class));
    }

    @Test
    @DisplayName("트랜잭션 동기화가 활성화된 경우 afterCommit 전에는 dispatcher를 호출하지 않는다")
    void acceptLinkJob_defersDispatchUntilAfterCommit() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobOutboxRepository, userService, scrapeJobOutboxAfterCommitExecutor);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scrapeJobOutboxRepository.save(any(ScrapeJobOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(scrapeJobOutboxAfterCommitExecutor).dispatchAsync(any(), any());

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.acceptJob(userId, "idem-1", request);

            verify(scrapeJobOutboxAfterCommitExecutor, never()).dispatchAsync(any(), any());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(scrapeJobOutboxAfterCommitExecutor).dispatchAsync(any(), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("같은 idempotency key와 같은 요청이면 기존 job을 재사용한다")
    void acceptLinkJob_reusesExistingJobForSameFingerprint() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobOutboxRepository, userService, scrapeJobOutboxAfterCommitExecutor);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");
        ScrapeJob existingJob = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                PortalLinkJobService.createRequestFingerprint("suwon", "17019013", "pw", ScrapeJobOperationType.LINK),
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.of(existingJob));

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.job_id()).isEqualTo(existingJob.getJobId());
        verify(scrapeJobRepository, never()).save(any(ScrapeJob.class));
        verify(scrapeJobOutboxRepository, never()).save(any(ScrapeJobOutbox.class));
    }

    @Test
    @DisplayName("같은 idempotency key에 다른 요청이면 409 예외를 던진다")
    void acceptLinkJob_throwsConflictWhenFingerprintDiffers() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobOutboxRepository, userService, scrapeJobOutboxAfterCommitExecutor);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "changed");
        ScrapeJob existingJob = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                PortalLinkJobService.createRequestFingerprint("suwon", "17019013", "pw", ScrapeJobOperationType.LINK),
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.of(existingJob));

        assertThatThrownBy(() -> service.acceptJob(userId, "idem-1", request))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.code()));
    }

    @Test
    @DisplayName("outbox 저장 실패 시 예외를 던지고 요청을 실패 처리한다")
    void acceptLinkJob_throwsWhenOutboxSaveFails() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobOutboxRepository, userService, scrapeJobOutboxAfterCommitExecutor);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scrapeJobOutboxRepository.save(any(ScrapeJobOutbox.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.acceptJob(userId, "idem-1", request))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED.code()));
    }

    private static User disconnectedUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("disconnected@example.com")
                .profileNickname("tester")
                .build();
    }
}

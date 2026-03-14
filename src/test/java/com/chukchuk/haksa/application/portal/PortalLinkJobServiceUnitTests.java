package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLinkJobServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ScrapeJobPublisher scrapeJobPublisher;

    @Mock
    private UserService userService;

    @Test
    @DisplayName("새 idempotency key 요청이면 job을 생성하고 enqueue한다")
    void acceptLinkJob_createsJobAndPublishesMessage() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobPublisher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortalLinkDto.AcceptedResponse response = service.acceptJob(userId, "idem-1", request);

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(response.job_id()).isNotBlank();
        verify(scrapeJobPublisher).publish(any(ScrapeJobMessage.class));
    }

    @Test
    @DisplayName("같은 idempotency key와 같은 요청이면 기존 job을 재사용한다")
    void acceptLinkJob_reusesExistingJobForSameFingerprint() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobPublisher, userService);
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
        verify(scrapeJobPublisher, never()).publish(any(ScrapeJobMessage.class));
        verify(scrapeJobRepository, never()).save(any(ScrapeJob.class));
    }

    @Test
    @DisplayName("같은 idempotency key에 다른 요청이면 409 예외를 던진다")
    void acceptLinkJob_throwsConflictWhenFingerprintDiffers() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobPublisher, userService);
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
    @DisplayName("enqueue 실패 시 저장된 job을 삭제하고 예외를 던진다")
    void acceptLinkJob_deletesSavedJobWhenPublishFails() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobService service = new PortalLinkJobService(scrapeJobRepository, scrapeJobPublisher, userService);
        PortalLinkDto.LinkRequest request = new PortalLinkDto.LinkRequest("suwon", "17019013", "pw");

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser(userId));
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scrapeJobPublisher.publish(any(ScrapeJobMessage.class))).thenThrow(new RuntimeException("sqs down"));

        assertThatThrownBy(() -> service.acceptJob(userId, "idem-1", request))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED.code()));

        ArgumentCaptor<ScrapeJob> captor = ArgumentCaptor.forClass(ScrapeJob.class);
        verify(scrapeJobRepository).delete(captor.capture());
        assertThat(captor.getValue().getStatus()).isNotNull();
    }

    private static User disconnectedUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("disconnected@example.com")
                .profileNickname("tester")
                .build();
    }
}

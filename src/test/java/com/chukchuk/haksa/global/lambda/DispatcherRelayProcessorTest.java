package com.chukchuk.haksa.global.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatcherRelayProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("queued job이면 RUNNING으로 전이 후 worker queue로 전달한다")
    void process_updatesRunningAndPublishes() throws Exception {
        DispatcherRelayProcessor.JobStatusStore jobStatusStore = mock(DispatcherRelayProcessor.JobStatusStore.class);
        DispatcherRelayProcessor.WorkerQueueSender workerQueueSender = mock(DispatcherRelayProcessor.WorkerQueueSender.class);
        DispatcherRelayProcessor processor = new DispatcherRelayProcessor(objectMapper, jobStatusStore, workerQueueSender);

        when(jobStatusStore.markRunningIfQueued("job-1"))
                .thenReturn(DispatcherRelayProcessor.JobStatusTransitionResult.UPDATED);

        processor.process(payload("job-1"), "request-msg-1");

        verify(jobStatusStore).markRunningIfQueued("job-1");
        verify(workerQueueSender).send(payload("job-1"), "user-1", "job-1");
    }

    @Test
    @DisplayName("이미 RUNNING 등으로 처리된 job이면 worker queue 재전송을 건너뛴다")
    void process_skipsAlreadyDispatchedJob() throws Exception {
        DispatcherRelayProcessor.JobStatusStore jobStatusStore = mock(DispatcherRelayProcessor.JobStatusStore.class);
        DispatcherRelayProcessor.WorkerQueueSender workerQueueSender = mock(DispatcherRelayProcessor.WorkerQueueSender.class);
        DispatcherRelayProcessor processor = new DispatcherRelayProcessor(objectMapper, jobStatusStore, workerQueueSender);

        when(jobStatusStore.markRunningIfQueued("job-1"))
                .thenReturn(DispatcherRelayProcessor.JobStatusTransitionResult.SKIPPED);

        processor.process(payload("job-1"), "request-msg-1");

        verify(workerQueueSender, never()).send(payload("job-1"), "user-1", "job-1");
    }

    @Test
    @DisplayName("worker queue 전송 실패면 RUNNING을 QUEUED로 복구한다")
    void process_revertsQueuedWhenWorkerPublishFails() throws Exception {
        DispatcherRelayProcessor.JobStatusStore jobStatusStore = mock(DispatcherRelayProcessor.JobStatusStore.class);
        DispatcherRelayProcessor.WorkerQueueSender workerQueueSender = mock(DispatcherRelayProcessor.WorkerQueueSender.class);
        DispatcherRelayProcessor processor = new DispatcherRelayProcessor(objectMapper, jobStatusStore, workerQueueSender);

        when(jobStatusStore.markRunningIfQueued("job-1"))
                .thenReturn(DispatcherRelayProcessor.JobStatusTransitionResult.UPDATED);
        when(workerQueueSender.send(payload("job-1"), "user-1", "job-1"))
                .thenThrow(new IllegalStateException("sqs down"));

        assertThatThrownBy(() -> processor.process(payload("job-1"), "request-msg-1"))
                .isInstanceOf(DispatcherRelayProcessor.RetryableDispatcherException.class);

        verify(jobStatusStore).revertToQueuedIfRunning("job-1");
    }

    @Test
    @DisplayName("job이 없으면 warn 대상이지만 재시도 없이 소비 완료 처리한다")
    void process_skipsWhenJobMissing() throws Exception {
        DispatcherRelayProcessor.JobStatusStore jobStatusStore = mock(DispatcherRelayProcessor.JobStatusStore.class);
        DispatcherRelayProcessor.WorkerQueueSender workerQueueSender = mock(DispatcherRelayProcessor.WorkerQueueSender.class);
        DispatcherRelayProcessor processor = new DispatcherRelayProcessor(objectMapper, jobStatusStore, workerQueueSender);

        when(jobStatusStore.markRunningIfQueued("job-1"))
                .thenReturn(DispatcherRelayProcessor.JobStatusTransitionResult.JOB_NOT_FOUND);

        processor.process(payload("job-1"), "request-msg-1");

        verify(workerQueueSender, never()).send(payload("job-1"), "user-1", "job-1");
        verify(jobStatusStore, never()).revertToQueuedIfRunning("job-1");
    }

    @Test
    @DisplayName("복구 SQL 실패도 호출자에게 전달한다")
    void process_surfacesRevertFailure() throws Exception {
        DispatcherRelayProcessor.JobStatusStore jobStatusStore = mock(DispatcherRelayProcessor.JobStatusStore.class);
        DispatcherRelayProcessor.WorkerQueueSender workerQueueSender = mock(DispatcherRelayProcessor.WorkerQueueSender.class);
        DispatcherRelayProcessor processor = new DispatcherRelayProcessor(objectMapper, jobStatusStore, workerQueueSender);

        when(jobStatusStore.markRunningIfQueued("job-1"))
                .thenReturn(DispatcherRelayProcessor.JobStatusTransitionResult.UPDATED);
        when(workerQueueSender.send(payload("job-1"), "user-1", "job-1"))
                .thenThrow(new IllegalStateException("sqs down"));
        org.mockito.Mockito.doThrow(new SQLException("db down"))
                .when(jobStatusStore).revertToQueuedIfRunning("job-1");

        assertThatThrownBy(() -> processor.process(payload("job-1"), "request-msg-1"))
                .isInstanceOf(SQLException.class);
    }

    private static String payload(String jobId) {
        return """
                {
                  "job_id":"%s",
                  "user_id":"user-1",
                  "portal_type":"suwon",
                  "request_payload":{"username":"17019013","password":"pw"},
                  "requested_at":"2026-04-20T00:00:00Z",
                  "message_group_id":"user-1",
                  "message_deduplication_id":"%s"
                }
                """.formatted(jobId, jobId);
    }
}

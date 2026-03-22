package com.chukchuk.haksa.domain.scrapejob.repository;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScrapeJobOutboxRepository extends JpaRepository<ScrapeJobOutbox, String> {

    Optional<ScrapeJobOutbox> findByJobId(String jobId);

    long countByStatus(ScrapeJobOutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from ScrapeJobOutbox outbox
            where outbox.outboxId = :outboxId
              and outbox.status in :statuses
              and outbox.nextAttemptAt <= :now
            """)
    Optional<ScrapeJobOutbox> findPublishTargetForUpdateByOutboxId(
            @Param("outboxId") String outboxId,
            @Param("statuses") Collection<ScrapeJobOutboxStatus> statuses,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from ScrapeJobOutbox outbox
            where outbox.status in :statuses
              and outbox.nextAttemptAt <= :now
            order by outbox.createdAt asc
            """)
    List<ScrapeJobOutbox> findPublishTargetsForUpdate(
            @Param("statuses") Collection<ScrapeJobOutboxStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from ScrapeJobOutbox outbox
            join ScrapeJob job on job.jobId = outbox.jobId
            where outbox.status = :outboxStatus
              and outbox.sentAt <= :sentBefore
              and job.status = :jobStatus
            order by outbox.sentAt asc
            """)
    List<ScrapeJobOutbox> findStaleSentTargetsForUpdate(
            @Param("outboxStatus") ScrapeJobOutboxStatus outboxStatus,
            @Param("sentBefore") Instant sentBefore,
            @Param("jobStatus") ScrapeJobStatus jobStatus,
            Pageable pageable
    );
}

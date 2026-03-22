package com.chukchuk.haksa.domain.scrapejob.repository;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, String> {

    Optional<ScrapeJob> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<ScrapeJob> findByJobIdAndUserId(String jobId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ScrapeJob job where job.jobId = :jobId")
    Optional<ScrapeJob> findForUpdateByJobId(@Param("jobId") String jobId);
}

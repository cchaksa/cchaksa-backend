package com.chukchuk.haksa.domain.syncjob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    // 비관적 락 및 경합 회피 적용
    @Query(value = "select * from sync_job where id = :id for update skip locked", nativeQuery = true)
    Optional<SyncJob> findByIdForUpdate(@Param("id") Long id);

    List<SyncJob> findAllByStatusAndCreatedAtBefore(JobStatus status, Instant createdBefore);
}

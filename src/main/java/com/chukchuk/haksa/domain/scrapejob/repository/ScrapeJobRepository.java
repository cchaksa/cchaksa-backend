package com.chukchuk.haksa.domain.scrapejob.repository;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 스크래핑 작업을 사용자·멱등성 키로 조회하고 상태 전이를 위해 잠그는 저장소다. */
public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, String> {

  /**
   * 사용자와 멱등성 키가 일치하는 스크래핑 작업을 조회한다.
   *
   * @param userId 사용자 식별자
   * @param idempotencyKey 멱등성 키
   * @return 사용자와 멱등성 키가 일치하면 포함한 선택값
   */
  Optional<ScrapeJob> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

  /**
   * 작업 식별자와 사용자가 일치하는 스크래핑 작업을 조회한다.
   *
   * @param jobId 작업 식별자
   * @param userId 사용자 식별자
   * @return 작업 식별자와 사용자가 일치하면 포함한 선택값
   */
  Optional<ScrapeJob> findByJobIdAndUserId(String jobId, UUID userId);

  /**
   * 상태 전이를 위해 스크래핑 작업을 쓰기 잠금으로 조회한다.
   *
   * @param jobId 작업 식별자
   * @return 잠금으로 조회한 작업이 있으면 포함한 선택값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select job from ScrapeJob job where job.jobId = :jobId")
  Optional<ScrapeJob> findForUpdateByJobId(@Param("jobId") String jobId);
}

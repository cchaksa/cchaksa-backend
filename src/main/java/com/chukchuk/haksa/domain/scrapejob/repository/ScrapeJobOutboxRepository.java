package com.chukchuk.haksa.domain.scrapejob.repository;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 스크래핑 작업 발행 아웃박스를 조회하고 잠금는 저장소이다. */
public interface ScrapeJobOutboxRepository extends JpaRepository<ScrapeJobOutbox, String> {

  /**
   * 스크래핑 작업에 연결된 아웃박스를 찾는다.
   *
   * @param jobId 작업 식별자
   * @return 연결된 아웃박스가 있으면 포함한 선택값
   */
  Optional<ScrapeJobOutbox> findByJobId(String jobId);

  /**
   * 아웃박스를 변경하기 위해 비관적 쓰기 잠금으로 조회한다.
   *
   * @param outboxId 아웃박스 식별자
   * @return 잠금을 획득한 아웃박스가 있으면 포함한 선택값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select outbox
            from ScrapeJobOutbox outbox
            where outbox.outboxId = :outboxId
      """)
  Optional<ScrapeJobOutbox> findForUpdateByOutboxId(@Param("outboxId") String outboxId);

  /**
   * 지정한 상태의 아웃박스 수를 반환한다.
   *
   * @param status 상태
   * @return 해당 상태의 아웃박스 수
   */
  long countByStatus(ScrapeJobOutboxStatus status);

  /**
   * 발행 가능한 상태이고 재시도 시각이 된 아웃박스를 잠그고 찾는다.
   *
   * @param outboxId 아웃박스 식별자
   * @param statuses 발행 가능한 아웃박스 상태
   * @param now 재시도 가능 여부를 판단할 기준 시각
   * @return 잠금을 획득한 발행 대상이 있으면 포함한 선택값
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select outbox
            from ScrapeJobOutbox outbox
            where outbox.outboxId = :outboxId
              and outbox.status in :statuses
              and outbox.nextAttemptAt <= :now
      """)
  Optional<ScrapeJobOutbox> findPublishTargetForUpdateByOutboxId(
      @Param("outboxId") String outboxId,
      @Param("statuses") Collection<ScrapeJobOutboxStatus> statuses,
      @Param("now") Instant now);

  /**
   * 재시도 시각이 된 발행 대상을 생성순으로 잠그고 조회한다.
   *
   * @param statuses 발행 가능한 아웃박스 상태
   * @param now 재시도 가능 여부를 판단할 기준 시각
   * @param pageable 한 번에 잠글 대상 수
   * @return 잠금을 획득한 발행 대상
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select outbox
            from ScrapeJobOutbox outbox
            where outbox.status in :statuses
              and outbox.nextAttemptAt <= :now
            order by outbox.createdAt asc
      """)
  List<ScrapeJobOutbox> findPublishTargetsForUpdate(
      @Param("statuses") Collection<ScrapeJobOutboxStatus> statuses,
      @Param("now") Instant now,
      Pageable pageable);

  /**
   * 전송 후 일정 시간 이상 작업이 완료되지 않은 아웃박스를 잠그고 조회한다.
   *
   * @param outboxStatus 아웃박스 상태
   * @param sentBefore 전송 지연을 판단할 기준 시각
   * @param jobStatus 작업 상태
   * @param pageable 한 번에 잠글 대상 수
   * @return 전송 시각이 오래된 순으로 잠금된 아웃박스
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
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
      Pageable pageable);
}

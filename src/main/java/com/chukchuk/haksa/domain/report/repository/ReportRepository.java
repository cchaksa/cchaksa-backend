// 사용자 문의 조회와 계정 생명주기 일괄 갱신을 담당한다.

package com.chukchuk.haksa.domain.report.repository;

import com.chukchuk.haksa.domain.report.model.Report;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 사용자별 문의를 최신순으로 조회하고 소유권·스냅샷을 갱신하는 저장소다. */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

  /**
   * 사용자의 문의를 생성 시각과 식별자 역순으로 조회한다.
   *
   * @param userId 문의 소유자 식별자
   * @param pageable 페이지 요청
   * @return 최신 문의부터 정렬된 페이지
   */
  @Query(
      """
      SELECT r FROM Report r
      WHERE r.userId = :userId
      ORDER BY r.createdAt DESC, r.id DESC
      """)
  Page<Report> findAllByUserIdOrderByCreatedAtDescIdDesc(
      @Param("userId") UUID userId, Pageable pageable);

  /**
   * 계정 병합 전에 기존 사용자의 문의 소유권을 현재 사용자에게 이전한다.
   *
   * @param sourceUserId 병합되어 제거될 사용자 식별자
   * @param targetUserId 문의를 이어받을 사용자 식별자
   * @return 갱신된 문의 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE reports
          SET user_id = :targetUserId, updated_at = CURRENT_TIMESTAMP
          WHERE user_id = :sourceUserId
          """,
      nativeQuery = true)
  int reassignOwner(
      @Param("sourceUserId") UUID sourceUserId, @Param("targetUserId") UUID targetUserId);

  /**
   * 탈퇴 사용자의 문의에서 구조화된 제출자 스냅샷을 제거한다.
   *
   * @param userId 탈퇴 사용자 식별자
   * @return 갱신된 문의 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE reports
          SET submitted_user_id = NULL,
              department_id = NULL,
              department_name = NULL,
              student_code = NULL,
              primary_major_id = NULL,
              primary_major_name = NULL,
              secondary_major_id = NULL,
              secondary_major_name = NULL,
              is_transfer_student = NULL,
              admission_year = NULL,
              graduation_requirement_status = 'UNKNOWN',
              updated_at = CURRENT_TIMESTAMP
          WHERE user_id = :userId
          """,
      nativeQuery = true)
  int anonymizeSubmitterSnapshot(@Param("userId") UUID userId);
}

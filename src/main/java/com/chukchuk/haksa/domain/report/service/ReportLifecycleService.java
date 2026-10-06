// 계정 병합과 탈퇴에 따른 문의 데이터 생명주기를 처리한다.

package com.chukchuk.haksa.domain.report.service;

import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 계정 생명주기와 문의 소유권·스냅샷의 정합성을 유지한다. */
@Service
@RequiredArgsConstructor
public class ReportLifecycleService {

  private final ReportRepository reportRepository;

  /**
   * 병합되는 계정의 문의 소유권을 유지되는 계정으로 이전한다.
   *
   * @param sourceUserId 제거될 사용자 식별자
   * @param targetUserId 유지될 사용자 식별자
   */
  @Transactional
  public void reassignOwner(UUID sourceUserId, UUID targetUserId) {
    reportRepository.reassignOwner(sourceUserId, targetUserId);
  }

  /**
   * 탈퇴 사용자의 문의에 저장된 구조화된 제출자 정보를 제거한다.
   *
   * @param userId 탈퇴 사용자 식별자
   */
  @Transactional
  public void anonymizeByUserId(UUID userId) {
    reportRepository.anonymizeSubmitterSnapshot(userId);
  }
}

package com.chukchuk.haksa.application.portal;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;
import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.failure;
import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.success;

import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* 포털 연동 초기화 유스케이스 실행 */
/** 최초 포털 연동 시 사용자에게 학생 정보를 연결하고 연동 상태를 저장한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitializePortalConnectionService {

  private final UserPortalConnectionRepository userPortalConnectionRepository;
  private final UserService userService;
  private final PortalStudentDataMapper portalStudentDataMapper;

  /**
   * 아직 연동되지 않은 사용자에게 포털 학생 정보를 연결한다.
   *
   * <p>이미 연동됐거나 유효한 학생 정보가 없으면 상태를 변경하지 않고 실패 결과를 반환한다.
   *
   * @param userId 사용자 식별자
   * @param portalData 학생 정보가 포함된 포털 조회 결과
   * @return 연동된 학번과 학생 정보 또는 연동하지 못한 사유
   * @throws RuntimeException 사용자 조회, 포털 데이터 변환 또는 연동 정보 저장 중 예상하지 못한 오류가 발생한 경우
   */
  @Transactional
  public PortalConnectionResult executeWithPortalData(UUID userId, PortalData portalData) {
    long t0 = LogTime.start();
    try {
      // 사용자 조회 및 포털 연동 여부 확인
      User user = userService.getUserById(userId);
      if (user.getPortalConnected()) {
        log.warn("[BIZ] portal.init.skipped userId={} reason=already_connected", userId);
        return failure("이미 포털 계정과 연동된 사용자입니다.");
      }

      if (portalData == null || portalData.student() == null) {
        log.warn("[BIZ] portal.init.fail userId={} reason=portal_data_null", userId);
        return failure("포털 데이터가 존재하지 않습니다.");
      }

      PortalStudentInfo raw = portalData.student();
      PortalStudentDataMapper.PortalStudentData portalStudentData =
          portalStudentDataMapper.toStudentData(raw);
      if (portalStudentData == null) {
        log.warn("[BIZ] portal.init.fail userId={} reason=student_data_mapping_failed", userId);
        return failure("포털 학생 정보 초기화 실패");
      }

      // 포털 연동 초기화
      userPortalConnectionRepository.initializePortalConnection(
          user, portalStudentData.studentData());

      long tookMs = LogTime.elapsedMs(t0);
      if (tookMs >= SLOW_MS) {
        log.info("[BIZ] portal.init.done userId={} took_ms={}", userId, tookMs);
      }
      return success(raw.studentCode(), portalStudentData.studentInfo());
    } catch (Exception e) {
      log.warn("[BIZ] portal.init.ex userId={} ex={}", userId, e.getClass().getSimpleName(), e);
      throw new RuntimeException("포털 연동 중 오류가 발생했습니다.", e);
    }
  }
}

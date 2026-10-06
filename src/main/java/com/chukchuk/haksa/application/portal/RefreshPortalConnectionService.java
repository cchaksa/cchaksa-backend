package com.chukchuk.haksa.application.portal;

import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.failure;
import static com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.success;

import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 포털 연동 사용자의 학생 정보를 최신 조회 결과로 갱신한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshPortalConnectionService {

  private final UserPortalConnectionRepository userPortalConnectionRepository;
  private final UserService userService;
  private final PortalStudentDataMapper portalStudentDataMapper;

  /**
   * 기존 연동 학번이 같은 경우에만 포털 학생 정보를 갱신한다.
   *
   * <p>미연동 사용자, 학번 불일치 또는 유효하지 않은 포털 데이터는 저장하지 않고 실패 결과로 반환한다.
   *
   * @param userId 사용자 식별자
   * @param portalData 최신 학생 정보가 포함된 포털 조회 결과
   * @return 갱신된 학번과 학생 정보 또는 갱신하지 못한 사유
   * @throws RuntimeException 사용자 조회, 포털 데이터 변환 또는 연동 정보 저장 중 예상하지 못한 오류가 발생한 경우
   */
  @Transactional
  public PortalConnectionResult executeWithPortalData(UUID userId, PortalData portalData) {

    try {
      User user = userService.getUserById(userId);

      if (!user.getPortalConnected()) {
        // 정상 흐름 X -> WARN
        log.warn("[BIZ] portal.conn.fail userId={} reason=not_connected", userId);
        return failure("아직 포털 계정과 연동되지 않은 사용자입니다.");
      }

      if (portalData == null || portalData.student() == null) {
        log.warn("[BIZ] portal.conn.fail userId={} reason=portal_data_null", userId);
        return failure("포털 데이터가 존재하지 않습니다.");
      }

      PortalStudentInfo raw = portalData.student();
      if (user.getStudent() == null
          || !Objects.equals(user.getStudent().getStudentCode(), raw.studentCode())) {
        log.warn("[BIZ] portal.conn.fail userId={} reason=student_code_mismatch", userId);
        return failure("포털 학번이 기존 연동 정보와 일치하지 않습니다.");
      }

      PortalStudentDataMapper.PortalStudentData portalStudentData =
          portalStudentDataMapper.toStudentData(raw);
      if (portalStudentData == null) {
        log.warn("[BIZ] portal.conn.fail userId={} reason=student_data_mapping_failed", userId);
        return failure("포털 학생 정보 초기화 실패");
      }

      userPortalConnectionRepository.refreshPortalConnection(user, portalStudentData.studentData());

      return success(raw.studentCode(), portalStudentData.studentInfo());

    } catch (Exception e) {
      log.warn("[BIZ] portal.conn.ex userId={} ex={}", userId, e.getClass().getSimpleName(), e);
      throw new RuntimeException("포털 연동 중 오류가 발생했습니다.", e);
    }
  }
}

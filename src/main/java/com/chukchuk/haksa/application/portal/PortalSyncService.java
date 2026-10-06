package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.domain.graduation.service.StudentGraduationProgressService;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 포털 학생·학사 데이터를 사용자 계정에 최초 연결하거나 최신 상태로 동기화한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSyncService {

  private final InitializePortalConnectionService initializePortalConnectionService;
  private final RefreshPortalConnectionService refreshPortalConnectionService;
  private final SyncAcademicRecordService syncAcademicRecordService;
  private final UserService userService;
  private final StudentService studentService;
  private final StudentGraduationProgressService studentGraduationProgressService;
  private final SyncDesignatedCourseService syncDesignatedCourseService;

  /**
   * 학번이 같은 기존 사용자를 병합한 뒤 포털 연결, 학사 기록 및 졸업 정보를 동기화한다.
   *
   * @param userId 사용자 식별자
   * @param portalData 학생·성적·졸업 정보가 포함된 포털 조회 결과
   * @param snapshotVersion 이번 스크래퍼 결과의 스냅샷 버전
   * @return 실제 반영 대상 사용자의 학생 정보와 성공 상태
   * @throws PortalScrapeException 포털 연결 또는 학사 기록 동기화에 실패한 경우
   */
  @Transactional
  public ScrapingResponse syncWithPortal(
      UUID userId, PortalData portalData, Instant snapshotVersion) {
    long t0 = LogTime.start();
    User mergedUser =
        userService.tryMergeWithExistingUser(userId, portalData.student().studentCode());
    UUID activeUserId = mergedUser.getId();
    lockStudentForSync(activeUserId);
    if (Boolean.TRUE.equals(mergedUser.getPortalConnected())) {
      log.info(
          "[BIZ] portal.sync.refresh_after_merge userId={} activeUserId={}", userId, activeUserId);
      return refreshActiveUserFromPortal(userId, mergedUser, portalData, snapshotVersion, t0);
    }

    // 1. 포털 초기화
    PortalConnectionResult conn =
        initializePortalConnectionService.executeWithPortalData(activeUserId, portalData);
    if (!conn.isSuccess()) {
      log.warn(
          "[BIZ] portal.sync.conn.fail userId={} msg={}",
          activeUserId,
          LogSanitizer.arg(conn.error()));
      throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
    }

    lockStudentForSync(activeUserId);

    // 2. 학업 이력 동기화
    SyncAcademicRecordResult sync =
        syncAcademicRecordService.executeWithPortalData(activeUserId, portalData);
    if (!sync.isSuccess()) {
      log.warn(
          "[BIZ] portal.sync.sync.fail userId={} msg={}",
          activeUserId,
          LogSanitizer.arg(sync.getError()));
      throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
    }

    // 3. 외국어 졸업 인증 동기화
    syncLanguageCert(activeUserId, portalData);

    // 4. 지정과목 스냅샷 동기화
    syncDesignatedCourseService.sync(activeUserId, portalData.designatedCourses(), snapshotVersion);

    // 5. 포털 연결 마킹
    User user = userService.getUserById(activeUserId);
    user.markPortalConnected(Instant.now());
    userService.save(user);
    studentService.markReconnectedByUser(user);

    long tookMs = LogTime.elapsedMs(t0);
    log.info(
        "[BIZ] portal.sync.done userId={} activeUserId={} took_ms={}",
        userId,
        activeUserId,
        tookMs);

    // 6. 응답 생성
    return ScrapingResponse.success(UUID.randomUUID().toString(), conn.studentInfo());
  }

  /**
   * 최신 포털 데이터로 기존 연동 정보와 학사 기록을 함께 갱신한다.
   *
   * @param userId 사용자 식별자
   * @param portalData 학생 및 학사 정보가 포함된 최신 포털 조회 결과
   * @param snapshotVersion 이번 스크래퍼 결과의 스냅샷 버전
   * @return 갱신 성공 여부와 학생 정보를 담은 응답
   */
  @Transactional
  public ScrapingResponse refreshFromPortal(
      UUID userId, PortalData portalData, Instant snapshotVersion) {
    long t0 = LogTime.start();
    User user = userService.getUserById(userId);
    return refreshActiveUserFromPortal(userId, user, portalData, snapshotVersion, t0);
  }

  private ScrapingResponse refreshActiveUserFromPortal(
      UUID userId, User activeUser, PortalData portalData, Instant snapshotVersion, long t0) {
    UUID activeUserId = activeUser.getId();
    lockStudentForSync(activeUserId);

    // 1. 포털 연동 정보 갱신
    PortalConnectionResult conn =
        refreshPortalConnectionService.executeWithPortalData(activeUserId, portalData);
    if (!conn.isSuccess()) {
      log.warn(
          "[BIZ] portal.refresh.conn.fail userId={} msg={}",
          activeUserId,
          LogSanitizer.arg(conn.error()));
      throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
    }

    // 2. 학업 이력 재동기화
    SyncAcademicRecordResult sync =
        syncAcademicRecordService.executeForRefreshPortalData(activeUserId, portalData);
    if (!sync.isSuccess()) {
      log.warn(
          "[BIZ] portal.refresh.sync.fail userId={} msg={}",
          activeUserId,
          LogSanitizer.arg(sync.getError()));
      throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
    }

    // 3. 외국어 졸업 인증 동기화
    syncLanguageCert(activeUserId, portalData);

    // 4. 지정과목 스냅샷 동기화
    syncDesignatedCourseService.sync(activeUserId, portalData.designatedCourses(), snapshotVersion);

    // 5. 마지막 동기화 시간만 업데이트 (포털 연결은 유지)
    activeUser.updateLastSyncedAt(Instant.now());
    userService.save(activeUser);
    studentService.markReconnectedByUser(activeUser);

    long tookMs = LogTime.elapsedMs(t0);
    log.info(
        "[BIZ] portal.refresh.done userId={} activeUserId={} took_ms={}",
        userId,
        activeUserId,
        tookMs);

    // 6. 응답 생성
    return ScrapingResponse.success(UUID.randomUUID().toString(), conn.studentInfo());
  }

  private void syncLanguageCert(UUID activeUserId, PortalData portalData) {
    Student student = studentService.getStudentByUserId(activeUserId);
    studentGraduationProgressService.syncLanguageCert(
        student, portalData.student().languageCertFulfilled());
  }

  private void lockStudentForSync(UUID userId) {
    studentService.findForUpdateByUserId(userId);
  }
}

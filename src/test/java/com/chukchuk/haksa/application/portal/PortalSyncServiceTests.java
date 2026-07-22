// 포털 동기화 분기와 외국어 졸업 인증 저장을 함께 검증하는 테스트
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.domain.graduation.service.StudentGraduationProgressService;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.infrastructure.portal.model.AdmissionInfo;
import com.chukchuk.haksa.infrastructure.portal.model.CodeName;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalSyncServiceTests {

    @Mock
    private InitializePortalConnectionService initializePortalConnectionService;

    @Mock
    private RefreshPortalConnectionService refreshPortalConnectionService;

    @Mock
    private SyncAcademicRecordService syncAcademicRecordService;

    @Mock
    private UserService userService;

    @Mock
    private StudentService studentService;

    @Mock
    private StudentGraduationProgressService studentGraduationProgressService;

    @Mock
    private Student student;

    private PortalSyncService portalSyncService;

    @BeforeEach
    void setUp() {
        portalSyncService = new PortalSyncService(
                initializePortalConnectionService,
                refreshPortalConnectionService,
                syncAcademicRecordService,
                userService,
                studentService,
                studentGraduationProgressService
        );
    }

    @Test
    @DisplayName("LINK 중 기존 연동 사용자가 병합되면 REFRESH처럼 재동기화한다")
    void syncWithPortal_usesRefreshFlowWhenMergeMakesUserConnected() {
        UUID userId = UUID.randomUUID();
        User mergedUser = connectedUser(userId);
        PortalData portalData = portalData("19018036", true);
        PortalConnectionResult refreshResult = successConnection("19018036");

        when(userService.tryMergeWithExistingUser(userId, "19018036")).thenReturn(mergedUser);
        when(refreshPortalConnectionService.executeWithPortalData(userId, portalData)).thenReturn(refreshResult);
        when(syncAcademicRecordService.executeForRefreshPortalData(userId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());

        var response = portalSyncService.syncWithPortal(userId, portalData);

        assertThat(response.status()).isEqualTo("SUCCESS");
        verify(initializePortalConnectionService, never()).executeWithPortalData(eq(userId), any());
        verify(refreshPortalConnectionService).executeWithPortalData(userId, portalData);
        verify(syncAcademicRecordService).executeForRefreshPortalData(userId, portalData);
        verify(syncAcademicRecordService, never()).executeWithPortalData(eq(userId), any());
        verify(userService).save(mergedUser);
        verify(studentService).markReconnectedByUser(mergedUser);
    }

    @Test
    @DisplayName("처음 LINK하는 사용자는 기존 신규 초기화 흐름을 유지한다")
    void syncWithPortal_keepsInitialFlowForUnconnectedUser() {
        UUID userId = UUID.randomUUID();
        User user = unconnectedUser(userId);
        PortalData portalData = portalData("19018036", true);
        PortalConnectionResult initialResult = successConnection("19018036");

        when(userService.tryMergeWithExistingUser(userId, "19018036")).thenReturn(user);
        when(initializePortalConnectionService.executeWithPortalData(userId, portalData)).thenReturn(initialResult);
        when(syncAcademicRecordService.executeWithPortalData(userId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());
        when(userService.getUserById(userId)).thenReturn(user);

        var response = portalSyncService.syncWithPortal(userId, portalData);

        assertThat(response.status()).isEqualTo("SUCCESS");
        verify(initializePortalConnectionService).executeWithPortalData(userId, portalData);
        verify(syncAcademicRecordService).executeWithPortalData(userId, portalData);
        verify(refreshPortalConnectionService, never()).executeWithPortalData(eq(userId), any());
        verify(syncAcademicRecordService, never()).executeForRefreshPortalData(eq(userId), any());
        verify(userService).save(user);
        verify(studentService).markReconnectedByUser(user);
    }

    @Test
    @DisplayName("초기 포털 연동 성공 시 외국어 인증 상태를 저장한다")
    void syncWithPortalStoresLanguageCertFulfilled() {
        UUID userId = UUID.randomUUID();
        UUID activeUserId = UUID.randomUUID();
        User activeUser = User.builder()
                .id(activeUserId)
                .email("active@example.com")
                .profileNickname("active")
                .build();
        PortalData portalData = portalData("17019013", true);

        when(userService.tryMergeWithExistingUser(userId, "17019013")).thenReturn(activeUser);
        when(initializePortalConnectionService.executeWithPortalData(activeUserId, portalData))
                .thenReturn(successConnection("17019013"));
        when(syncAcademicRecordService.executeWithPortalData(activeUserId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());
        when(studentService.getStudentByUserId(activeUserId)).thenReturn(student);
        when(userService.getUserById(activeUserId)).thenReturn(activeUser);

        portalSyncService.syncWithPortal(userId, portalData);

        verify(studentGraduationProgressService)
                .syncLanguageCert(eq(student), eq(true));
    }

    @Test
    @DisplayName("포털 새로고침 성공 시 외국어 인증 상태를 갱신한다")
    void refreshFromPortalStoresLanguageCertFulfilled() {
        UUID userId = UUID.randomUUID();
        User activeUser = User.builder()
                .id(userId)
                .email("active@example.com")
                .profileNickname("active")
                .build();
        PortalData portalData = portalData("17019013", false);

        when(userService.getUserById(userId)).thenReturn(activeUser);
        when(refreshPortalConnectionService.executeWithPortalData(userId, portalData))
                .thenReturn(successConnection("17019013"));
        when(syncAcademicRecordService.executeForRefreshPortalData(userId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());
        when(studentService.getStudentByUserId(userId)).thenReturn(student);

        portalSyncService.refreshFromPortal(userId, portalData);

        verify(userService, never()).tryMergeWithExistingUser(any(), any());
        verify(studentGraduationProgressService)
                .syncLanguageCert(eq(student), eq(false));
    }

    private static User connectedUser(UUID userId) {
        User user = unconnectedUser(userId);
        user.markPortalConnected(Instant.parse("2026-05-25T00:00:00Z"));
        return user;
    }

    private static User unconnectedUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();
    }

    private static PortalData portalData(String studentCode, Boolean languageCertFulfilled) {
        return new PortalData(
                new PortalStudentInfo(
                        studentCode,
                        "홍길동",
                        new CodeName("01", "수원대학교"),
                        new CodeName("D1", "컴퓨터학부"),
                        new CodeName("M1", "컴퓨터학과"),
                        null,
                        "재학",
                        new AdmissionInfo(2021, 10, "신입"),
                        new PortalAcademicInfo(4, 8, 120, 4.0),
                        languageCertFulfilled
                ),
                null,
                null
        );
    }

    private static PortalConnectionResult successConnection(String studentCode) {
        return PortalConnectionResult.success(
                studentCode,
                new PortalConnectionResult.StudentInfo(
                        "홍길동",
                        "수원대학교",
                        "컴퓨터학과",
                        studentCode,
                        4,
                        "재학",
                        1
                )
        );
    }
}

// 포털 연동 동기화 분기 정책을 검증하는 테스트
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
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

    private PortalSyncService portalSyncService;

    @BeforeEach
    void setUp() {
        portalSyncService = new PortalSyncService(
                initializePortalConnectionService,
                refreshPortalConnectionService,
                syncAcademicRecordService,
                userService,
                studentService
        );
    }

    @Test
    @DisplayName("LINK 중 기존 연동 사용자가 병합되면 REFRESH처럼 재동기화한다")
    void syncWithPortal_usesRefreshFlowWhenMergeMakesUserConnected() {
        UUID userId = UUID.randomUUID();
        User mergedUser = connectedUser(userId);
        PortalData portalData = portalData("19018036");
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
        PortalData portalData = portalData("19018036");
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

    private static PortalData portalData(String studentCode) {
        return new PortalData(
                new PortalStudentInfo(
                        studentCode,
                        "홍길동",
                        new CodeName("01", "수원대학교"),
                        new CodeName("D1", "컴퓨터학부"),
                        new CodeName("M1", "컴퓨터학과"),
                        null,
                        "재학",
                        null,
                        new PortalAcademicInfo(4, 8, 120, 4.0)
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

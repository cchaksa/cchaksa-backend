// 포털 동기화가 외국어 졸업 인증 상태를 함께 저장하는지 검증하는 테스트
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        PortalData portalData = portalData(true);
        PortalSyncService service = newService();

        when(userService.tryMergeWithExistingUser(userId, "17019013")).thenReturn(activeUser);
        when(initializePortalConnectionService.executeWithPortalData(activeUserId, portalData))
                .thenReturn(successConnection());
        when(syncAcademicRecordService.executeWithPortalData(activeUserId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());
        when(studentService.getStudentByUserId(activeUserId)).thenReturn(student);
        when(userService.getUserById(activeUserId)).thenReturn(activeUser);

        service.syncWithPortal(userId, portalData);

        verify(studentGraduationProgressService)
                .syncLanguageCert(eq(student), eq(true), any(Instant.class));
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
        PortalData portalData = portalData(false);
        PortalSyncService service = newService();

        when(userService.tryMergeWithExistingUser(userId, "17019013")).thenReturn(activeUser);
        when(refreshPortalConnectionService.executeWithPortalData(userId, portalData))
                .thenReturn(successConnection());
        when(syncAcademicRecordService.executeForRefreshPortalData(userId, portalData))
                .thenReturn(SyncAcademicRecordResult.success());
        when(studentService.getStudentByUserId(userId)).thenReturn(student);
        when(userService.getUserById(userId)).thenReturn(activeUser);

        service.refreshFromPortal(userId, portalData);

        verify(studentGraduationProgressService)
                .syncLanguageCert(eq(student), eq(false), any(Instant.class));
    }

    private PortalSyncService newService() {
        return new PortalSyncService(
                initializePortalConnectionService,
                refreshPortalConnectionService,
                syncAcademicRecordService,
                userService,
                studentService,
                studentGraduationProgressService
        );
    }

    private static PortalConnectionResult successConnection() {
        return PortalConnectionResult.success(
                "17019013",
                new PortalConnectionResult.StudentInfo(
                        "홍길동",
                        "수원대학교",
                        "컴퓨터학과",
                        "17019013",
                        4,
                        "재학",
                        1
                )
        );
    }

    private static PortalData portalData(Boolean languageCertFulfilled) {
        return new PortalData(
                new PortalStudentInfo(
                        "17019013",
                        "홍길동",
                        new CodeName("01", "수원대학교"),
                        new CodeName("D1", "컴퓨터학부"),
                        new CodeName("M1", "컴퓨터학과"),
                        null,
                        "재학",
                        new AdmissionInfo(2021, 10, "신입"),
                        new PortalAcademicInfo(4, 8, 120, 3.8),
                        languageCertFulfilled
                ),
                null,
                null
        );
    }
}

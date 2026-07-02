// 포털 연동 데이터 경계의 방어 처리를 검증한다.
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.service.DepartmentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserPortalConnectionRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.infrastructure.portal.model.AdmissionInfo;
import com.chukchuk.haksa.infrastructure.portal.model.CodeName;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalConnectionGuardTests {

    @Mock
    private DepartmentService departmentService;

    @Mock
    private UserPortalConnectionRepository userPortalConnectionRepository;

    @Mock
    private UserService userService;

    @Test
    void mapperReturnsNullWhenRequiredPortalBlocksAreMissing() {
        PortalStudentDataMapper mapper = new PortalStudentDataMapper(departmentService);

        assertThat(mapper.toStudentData(null)).isNull();
        assertThat(mapper.toStudentData(student("재학", null, admission(), academic()))).isNull();
        assertThat(mapper.toStudentData(student("재학", department(), null, academic()))).isNull();
        assertThat(mapper.toStudentData(student("재학", department(), admission(), null))).isNull();
        verifyNoInteractions(departmentService);
    }

    @Test
    void mapperReturnsNullWhenStudentStatusIsUnknown() {
        PortalStudentDataMapper mapper = new PortalStudentDataMapper(departmentService);

        assertThat(mapper.toStudentData(student("UNKNOWN", department(), admission(), academic()))).isNull();
        verifyNoInteractions(departmentService);
    }

    @Test
    void initializeReturnsFailureWhenPortalDataIsMissing() {
        UUID userId = UUID.randomUUID();
        InitializePortalConnectionService service = new InitializePortalConnectionService(
                userPortalConnectionRepository,
                userService,
                new PortalStudentDataMapper(departmentService)
        );
        when(userService.getUserById(userId)).thenReturn(user(userId, false));

        PortalConnectionResult result = service.executeWithPortalData(userId, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("포털 데이터가 존재하지 않습니다.");
        verify(userPortalConnectionRepository, never()).initializePortalConnection(any(), any());
    }

    @Test
    void refreshReturnsFailureWhenPortalStudentIsMissing() {
        UUID userId = UUID.randomUUID();
        RefreshPortalConnectionService service = new RefreshPortalConnectionService(
                userPortalConnectionRepository,
                userService,
                new PortalStudentDataMapper(departmentService)
        );
        when(userService.getUserById(userId)).thenReturn(user(userId, true));

        PortalConnectionResult result = service.executeWithPortalData(userId, new PortalData(null, null, null));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("포털 데이터가 존재하지 않습니다.");
        verify(userPortalConnectionRepository, never()).refreshPortalConnection(any(), any());
    }

    private static User user(UUID userId, boolean connected) {
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();
        if (connected) {
            user.markPortalConnected(Instant.parse("2026-05-25T00:00:00Z"));
        }
        return user;
    }

    private static PortalStudentInfo student(
            String status,
            CodeName department,
            AdmissionInfo admission,
            PortalAcademicInfo academic
    ) {
        return new PortalStudentInfo(
                "19018036",
                "홍길동",
                new CodeName("01", "수원대학교"),
                department,
                null,
                null,
                status,
                admission,
                academic,
                null
        );
    }

    private static CodeName department() {
        return new CodeName("D1", "컴퓨터학부");
    }

    private static AdmissionInfo admission() {
        return new AdmissionInfo(2021, 10, "신입");
    }

    private static PortalAcademicInfo academic() {
        return new PortalAcademicInfo(4, 8, 120, 4.0);
    }
}

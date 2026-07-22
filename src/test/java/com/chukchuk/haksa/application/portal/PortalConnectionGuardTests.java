// 포털 연동 데이터 경계의 방어 처리를 검증한다.
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.service.DepartmentService;
import com.chukchuk.haksa.domain.student.model.Student;
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

    @Mock
    private Student student;

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

    @Test
    void refreshReturnsFailureWhenStudentCodeDiffersFromCurrentStudent() {
        UUID userId = UUID.randomUUID();
        RefreshPortalConnectionService service = new RefreshPortalConnectionService(
                userPortalConnectionRepository,
                userService,
                new PortalStudentDataMapper(departmentService)
        );
        User user = user(userId, true);
        user.setStudent(student);
        when(student.getStudentCode()).thenReturn("19018036");
        when(userService.getUserById(userId)).thenReturn(user);

        PortalConnectionResult result = service.executeWithPortalData(
                userId,
                new PortalData(student("재학", department(), admission(), academic(), "24028036"), null, null)
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).isEqualTo("포털 학번이 기존 연동 정보와 일치하지 않습니다.");
        verify(userPortalConnectionRepository, never()).refreshPortalConnection(any(), any());
        verifyNoInteractions(departmentService);
    }

    @Test
    void refreshUpdatesCurrentStudentWhenStudentCodeMatches() {
        UUID userId = UUID.randomUUID();
        RefreshPortalConnectionService service = new RefreshPortalConnectionService(
                userPortalConnectionRepository,
                userService,
                new PortalStudentDataMapper(departmentService)
        );
        User user = user(userId, true);
        user.setStudent(student);
        when(student.getStudentCode()).thenReturn("19018036");
        when(userService.getUserById(userId)).thenReturn(user);
        when(departmentService.getOrCreateDepartment("D1", "컴퓨터학부"))
                .thenReturn(new Department("D1", "컴퓨터학부"));

        PortalConnectionResult result = service.executeWithPortalData(
                userId,
                new PortalData(student("재학", department(), admission(), academic()), null, null)
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.studentId()).isEqualTo("19018036");
        verify(userPortalConnectionRepository).refreshPortalConnection(any(), any());
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
        return student(status, department, admission, academic, "19018036");
    }

    private static PortalStudentInfo student(
            String status,
            CodeName department,
            AdmissionInfo admission,
            PortalAcademicInfo academic,
            String studentCode
    ) {
        return new PortalStudentInfo(
                studentCode,
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

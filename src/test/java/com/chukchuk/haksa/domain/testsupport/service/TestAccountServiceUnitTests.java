// dev 테스트 계정 생성 서비스 동작을 검증하는 테스트

package com.chukchuk.haksa.domain.testsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.testsupport.dto.TestDataDto;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TestAccountServiceUnitTests {

  @Mock private UserRepository userRepository;

  @Mock private StudentRepository studentRepository;

  @Mock private DepartmentRepository departmentRepository;

  @Mock private JwtProvider jwtProvider;

  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private TestAccountService accountService;

  @Test
  @DisplayName("테스트 계정 생성 시 test_ prefix 계정과 토큰을 만든다")
  void createTestUserCreatesPrefixedAccountAndTokens() {
    Department department = new Department("CSE", "컴퓨터학과");
    TestDataDto.CreateTestUserRequest request =
        new TestDataDto.CreateTestUserRequest("프론트테스트", 1L, 1L, null, 2024);
    when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(studentRepository.save(any(Student.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("access-token");
    Date refreshTokenExpiresAt = new Date();
    when(jwtProvider.createRefreshToken(any()))
        .thenReturn(
            new AuthDto.RefreshTokenWithExpiry(
                "refresh-token", refreshTokenExpiresAt, "session-1"));

    TestDataDto.TestUserResponse response = accountService.createTestUser(request);

    assertThat(response.email()).startsWith("test_");
    assertThat(response.studentCode()).startsWith("test_");
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
    verify(userRepository).save(userCaptor.capture());
    verify(studentRepository).save(studentCaptor.capture());
    verify(refreshTokenService)
        .save(
            "session-1",
            userCaptor.getValue().getId().toString(),
            "refresh-token",
            refreshTokenExpiresAt);
    assertThat(userCaptor.getValue().getEmail()).startsWith("test_");
    assertThat(userCaptor.getValue().getPortalConnected()).isTrue();
    assertThat(userCaptor.getValue().getConnectedAt()).isNotNull();
    assertThat(userCaptor.getValue().getLastSyncedAt()).isNotNull();
    assertThat(studentCaptor.getValue().getStudentCode()).startsWith("test_");
    assertThat(studentCaptor.getValue().isTransferStudent()).isFalse();
  }

  @Test
  @DisplayName("학과 ID가 없으면 기본 학과 조회를 1건으로 제한한다")
  void createTestUserWithoutDepartmentIdLimitsDefaultDepartmentLookup() {
    Department department = new Department("CSE", "컴퓨터학과");
    final TestDataDto.CreateTestUserRequest request =
        new TestDataDto.CreateTestUserRequest("프론트테스트", null, null, null, 2024, null);
    when(departmentRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(department)));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(studentRepository.save(any(Student.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("access-token");
    Date refreshTokenExpiresAt = new Date();
    when(jwtProvider.createRefreshToken(any()))
        .thenReturn(
            new AuthDto.RefreshTokenWithExpiry(
                "refresh-token", refreshTokenExpiresAt, "session-1"));

    accountService.createTestUser(request);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(departmentRepository).findAll(pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
  }

  @Test
  @DisplayName("포털 미연동 옵션이면 테스트 계정을 포털 미연동 상태로 만든다")
  void createTestUserWithPortalLinkedFalseKeepsUserUnlinked() {
    Department department = new Department("CSE", "컴퓨터학과");
    final TestDataDto.CreateTestUserRequest request =
        new TestDataDto.CreateTestUserRequest("프론트테스트", 1L, 1L, null, 2024, false);
    when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(studentRepository.save(any(Student.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtProvider.createAccessToken(any(), any(), any())).thenReturn("access-token");
    Date refreshTokenExpiresAt = new Date();
    when(jwtProvider.createRefreshToken(any()))
        .thenReturn(
            new AuthDto.RefreshTokenWithExpiry(
                "refresh-token", refreshTokenExpiresAt, "session-1"));

    accountService.createTestUser(request);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getPortalConnected()).isFalse();
    assertThat(userCaptor.getValue().getConnectedAt()).isNull();
    assertThat(userCaptor.getValue().getLastSyncedAt()).isNull();
  }
}

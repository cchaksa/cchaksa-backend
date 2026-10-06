// 지정과목 최신 스냅샷 동기화 정책을 검증한다.

package com.chukchuk.haksa.application.portal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncDesignatedCourseServiceTests {

  @Mock private StudentRepository studentRepository;

  @Mock private StudentDesignatedCourseRepository designatedCourseRepository;

  @Mock private AcademicCache academicCache;

  @Mock private Student student;

  private SyncDesignatedCourseService service;

  @BeforeEach
  void setUp() {
    service =
        new SyncDesignatedCourseService(
            studentRepository, designatedCourseRepository, academicCache);
  }

  @Test
  @DisplayName("지정과목 필드가 수신되지 않으면 기존 데이터와 버전을 건드리지 않는다")
  void skipsWhenDesignatedCoursesWereNotReceived() {
    service.sync(UUID.randomUUID(), DesignatedCourseSnapshot.notReceived(), version("02:00"));

    verifyNoInteractions(studentRepository, designatedCourseRepository);
  }

  @Test
  @DisplayName("최신 지정과목 배열은 기존 데이터를 삭제하고 순서와 중복을 포함해 저장한다")
  void replacesWithLatestCourses() {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Instant snapshotVersion = version("02:00");
    when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(student));
    when(student.getId()).thenReturn(studentId);
    when(student.canApplyDesignatedCourseSnapshot(snapshotVersion)).thenReturn(true);
    DesignatedCourseData first = course("C101", 0);
    DesignatedCourseData duplicate = course("C101", 1);

    service.sync(
        userId, DesignatedCourseSnapshot.received(List.of(first, duplicate)), snapshotVersion);

    verify(designatedCourseRepository).deleteAllByStudentId(studentId);
    ArgumentCaptor<List<StudentDesignatedCourse>> captor = ArgumentCaptor.forClass(List.class);
    verify(designatedCourseRepository).saveAll(captor.capture());
    Assertions.assertThat(captor.getValue())
        .extracting(StudentDesignatedCourse::getSourceOrder)
        .containsExactly(0, 1);
    verify(student).updateDesignatedCourseSnapshotVersion(snapshotVersion);
    verify(academicCache).deleteAllByStudentId(studentId);
  }

  @Test
  @DisplayName("빈 지정과목 배열은 기존 데이터를 삭제하고 빈 목록을 저장하지 않는다")
  void clearsWhenEmptyArrayReceived() {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Instant snapshotVersion = version("02:00");
    when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(student));
    when(student.getId()).thenReturn(studentId);
    when(student.canApplyDesignatedCourseSnapshot(snapshotVersion)).thenReturn(true);

    service.sync(userId, DesignatedCourseSnapshot.received(List.of()), snapshotVersion);

    verify(designatedCourseRepository).deleteAllByStudentId(studentId);
    verify(designatedCourseRepository, never()).saveAll(any());
    verify(student).updateDesignatedCourseSnapshotVersion(snapshotVersion);
    verify(academicCache).deleteAllByStudentId(studentId);
  }

  @Test
  @DisplayName("동일하거나 과거인 지정과목 스냅샷은 기존 데이터를 유지한다")
  void skipsStaleSnapshot() {
    UUID userId = UUID.randomUUID();
    Instant snapshotVersion = version("02:00");
    when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(student));
    when(student.canApplyDesignatedCourseSnapshot(snapshotVersion)).thenReturn(false);

    service.sync(
        userId, DesignatedCourseSnapshot.received(List.of(course("C101", 0))), snapshotVersion);

    verify(designatedCourseRepository, never()).deleteAllByStudentId(any());
    verify(designatedCourseRepository, never()).saveAll(any());
    verify(student, never()).updateDesignatedCourseSnapshotVersion(any());
  }

  @Test
  @DisplayName("지정과목 수신 시 학생이 없으면 STUDENT_NOT_FOUND를 던진다")
  void throwsWhenStudentMissing() {
    UUID userId = UUID.randomUUID();
    when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.sync(
                    userId, DesignatedCourseSnapshot.received(List.of()), version("02:00")))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("지정과목 수신 시 스냅샷 버전이 없으면 입력을 거부한다")
  void rejectsMissingSnapshotVersion() {
    assertThatThrownBy(
            () ->
                service.sync(UUID.randomUUID(), DesignatedCourseSnapshot.received(List.of()), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("스냅샷 버전");
  }

  @Test
  @DisplayName("탈퇴한 사용자의 늦은 지정과목 스냅샷은 저장하지 않는다")
  void skipsSnapshotForWithdrawnUser() {
    UUID userId = UUID.randomUUID();
    User withdrawnUser =
        User.builder().email("withdrawn@example.com").profileNickname("user").build();
    withdrawnUser.withdraw(Instant.parse("2026-08-30T01:00:00Z"));
    when(studentRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(student));
    when(student.getUser()).thenReturn(withdrawnUser);

    service.sync(
        userId, DesignatedCourseSnapshot.received(List.of(course("C101", 0))), version("02:00"));

    verify(designatedCourseRepository, never()).deleteAllByStudentId(any());
    verify(designatedCourseRepository, never()).saveAll(any());
    verify(student, never()).updateDesignatedCourseSnapshotVersion(any());
    verifyNoInteractions(academicCache);
  }

  private static Instant version(String time) {
    return Instant.parse("2026-08-30T" + time + ":00Z");
  }

  private static DesignatedCourseData course(String code, int sourceOrder) {
    return new DesignatedCourseData(
        "01", code, "자료구조", 3, "TRANSFER", 2024, "1학기", "", sourceOrder);
  }
}

// 학생 도메인의 편입생 판별 규칙을 검증한다.

package com.chukchuk.haksa.domain.student.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StudentModelTests {

  @Test
  @DisplayName("저장된 편입 여부가 false이면 학번 패턴과 관계없이 일반 학생으로 판별한다")
  void usesStoredFalseTransferFlag() {
    Student student =
        Student.builder()
            .studentCode("20221234")
            .admissionYear(2022)
            .isTransferStudent(false)
            .build();

    assertThat(student.isTransferStudent()).isFalse();
  }

  @Test
  @DisplayName("저장된 편입 여부가 true이면 학번 패턴과 관계없이 편입생으로 판별한다")
  void usesStoredTrueTransferFlag() {
    Student student =
        Student.builder()
            .studentCode("22221234")
            .admissionYear(2022)
            .isTransferStudent(true)
            .build();

    assertThat(student.isTransferStudent()).isTrue();
  }

  @Test
  @DisplayName("지정과목 스냅샷은 저장된 버전보다 최신일 때만 적용한다")
  void appliesOnlyNewerDesignatedCourseSnapshot() {
    Student student = Student.builder().studentCode("20221234").admissionYear(2022).build();
    Instant version = Instant.parse("2026-08-30T02:00:00Z");

    assertThat(student.canApplyDesignatedCourseSnapshot(version)).isTrue();

    student.updateDesignatedCourseSnapshotVersion(version);

    assertThat(student.canApplyDesignatedCourseSnapshot(Instant.parse("2026-08-30T01:00:00Z")))
        .isFalse();
    assertThat(student.canApplyDesignatedCourseSnapshot(version)).isFalse();
    assertThat(student.canApplyDesignatedCourseSnapshot(Instant.parse("2026-08-30T03:00:00Z")))
        .isTrue();
  }

  @Test
  @DisplayName("지정과목 스냅샷 버전을 초기화할 수 있다")
  void clearsDesignatedCourseSnapshotVersion() {
    Student student = Student.builder().studentCode("20221234").admissionYear(2022).build();
    student.updateDesignatedCourseSnapshotVersion(Instant.parse("2026-08-30T02:00:00Z"));

    student.clearDesignatedCourseSnapshotVersion();

    assertThat(student.canApplyDesignatedCourseSnapshot(Instant.parse("2026-08-30T01:00:00Z")))
        .isTrue();
  }

  @Test
  @DisplayName("지정과목 초기화 시 초기화 이전 스냅샷은 다시 적용하지 않는다")
  void blocksSnapshotsCreatedBeforeReset() {
    Student student = Student.builder().studentCode("20221234").admissionYear(2022).build();
    Instant resetAt = Instant.parse("2026-08-30T02:00:00Z");

    student.resetDesignatedCourseSnapshot(resetAt);

    assertThat(student.canApplyDesignatedCourseSnapshot(Instant.parse("2026-08-30T01:00:00Z")))
        .isFalse();
    assertThat(student.canApplyDesignatedCourseSnapshot(Instant.parse("2026-08-30T03:00:00Z")))
        .isTrue();
  }
}

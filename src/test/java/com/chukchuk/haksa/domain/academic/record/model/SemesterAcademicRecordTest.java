package com.chukchuk.haksa.domain.academic.record.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemesterAcademicRecordTest {

  @Test
  void markLectureEvaluationNotReleasedChangesNullStatusToNotReleased() {
    SemesterAcademicRecord record = semesterRecord();

    record.markLectureEvaluationNotReleased();

    assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.NOT_RELEASED);
  }

  @Test
  void markLectureEvaluationPendingChangesNotReleasedStatusToPending() {
    SemesterAcademicRecord record = semesterRecord();
    record.markLectureEvaluationNotReleased();

    record.markLectureEvaluationPending();

    assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.PENDING);
  }

  @Test
  void markLectureEvaluationNotReleasedDoesNotOverwriteCompletedStatus() {
    SemesterAcademicRecord record = semesterRecord();
    record.markLectureEvaluationCompleted();

    record.markLectureEvaluationNotReleased();

    assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.COMPLETED);
  }

  private SemesterAcademicRecord semesterRecord() {
    return new SemesterAcademicRecord(null, 2026, 10, 3, 0, null, null, null, null, null);
  }
}

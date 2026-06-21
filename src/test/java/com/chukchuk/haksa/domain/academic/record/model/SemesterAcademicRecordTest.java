package com.chukchuk.haksa.domain.academic.record.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SemesterAcademicRecordTest {

    @Test
    void markLectureEvaluationNotReleased_changesNullStatusToNotReleased() {
        SemesterAcademicRecord record = semesterRecord();

        record.markLectureEvaluationNotReleased();

        assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.NOT_RELEASED);
    }

    @Test
    void markLectureEvaluationPending_changesNotReleasedStatusToPending() {
        SemesterAcademicRecord record = semesterRecord();
        record.markLectureEvaluationNotReleased();

        record.markLectureEvaluationPending();

        assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.PENDING);
    }

    @Test
    void markLectureEvaluationNotReleased_doesNotOverwriteCompletedStatus() {
        SemesterAcademicRecord record = semesterRecord();
        record.markLectureEvaluationCompleted();

        record.markLectureEvaluationNotReleased();

        assertThat(record.getLectureEvaluationStatus()).isEqualTo(LectureEvaluationStatus.COMPLETED);
    }

    private SemesterAcademicRecord semesterRecord() {
        return new SemesterAcademicRecord(
                null,
                2026,
                10,
                3,
                0,
                null,
                null,
                null,
                null,
                null
        );
    }
}

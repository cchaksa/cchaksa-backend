package com.chukchuk.haksa.domain.lectureevaluations.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LectureEvaluationTagTest {

    @Test
    void lectureEvaluationTagsHaveKoreanLabels() {
        Map<LectureEvaluationTag, String> labels = Arrays.stream(LectureEvaluationTag.values())
                .collect(Collectors.toMap(tag -> tag, LectureEvaluationTag::getLabel));

        assertThat(labels).containsExactlyInAnyOrderEntriesOf(Map.of(
                LectureEvaluationTag.LOW_HOMEWORK, "과제가 적어요",
                LectureEvaluationTag.LOW_TEAM_PROJECT, "팀플이 적어요",
                LectureEvaluationTag.ONLINE_EXAM, "온라인 시험이에요",
                LectureEvaluationTag.EXAM_REPLACED_BY_HOMEWORK, "시험이 대체과제에요",
                LectureEvaluationTag.INTERESTING_LECTURE, "수업이 재밌어요",
                LectureEvaluationTag.INFORMATIVE_LECTURE, "강의가 유익해요",
                LectureEvaluationTag.ABSOLUTE_EXAM, "절대평가에요",
                LectureEvaluationTag.EASY_GRADE, "학점 따기 쉬워요"
        ));
    }
}

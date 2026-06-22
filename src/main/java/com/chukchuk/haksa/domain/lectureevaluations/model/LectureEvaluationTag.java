package com.chukchuk.haksa.domain.lectureevaluations.model;

public enum LectureEvaluationTag {
    LOW_HOMEWORK("과제가 적어요"),
    LOW_TEAM_PROJECT("팀플이 적어요"),
    ONLINE_EXAM("온라인 시험이에요"),
    EXAM_REPLACED_BY_HOMEWORK("시험이 대체과제에요"),
    INTERESTING_LECTURE("수업이 재밌어요"),
    INFORMATIVE_LECTURE("강의가 유익해요"),
    ABSOLUTE_EXAM("절대평가에요"),
    EASY_GRADE("학점 따기 쉬워요");

    private final String label;

    LectureEvaluationTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

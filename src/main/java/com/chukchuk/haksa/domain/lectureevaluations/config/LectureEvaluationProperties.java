package com.chukchuk.haksa.domain.lectureevaluations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lecture-evaluation")
public class LectureEvaluationProperties {

    private Integer targetYear;
    private Integer targetSemester;

    public LectureEvaluationProperties() {
    }

    public LectureEvaluationProperties(Integer targetYear, Integer targetSemester) {
        this.targetYear = targetYear;
        this.targetSemester = targetSemester;
    }

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }

    public Integer getTargetSemester() {
        return targetSemester;
    }

    public void setTargetSemester(Integer targetSemester) {
        this.targetSemester = targetSemester;
    }
}

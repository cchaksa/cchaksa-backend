package com.chukchuk.haksa.domain.student.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Grade {

    @Convert(converter = GradeTypeConverter.class)
    @Column(name = "grade")
    private GradeType value;

    protected Grade() {} // JPA를 위한 기본 생성자

    public Grade(GradeType value) {
        this.value = value;
    }

    public static Grade createInProgress() {
        return new Grade(GradeType.IP);
    }

    public GradeType getValue() {
        return value;
    }

    public boolean isCompleted() {
        return value != GradeType.IP;
    }

    public boolean isPassingGrade() {
        return value != GradeType.F && value != GradeType.NP && value != GradeType.IP;
    }

    public double getGradePoint() {
        return value.getGradePoint();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Grade g)) return false;
        return value == g.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
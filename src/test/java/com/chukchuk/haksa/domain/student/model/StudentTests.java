package com.chukchuk.haksa.domain.student.model;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTests {

    @Test
    @DisplayName("학사 연관관계 컬렉션을 초기화한다")
    void clearAcademicAssociations_clearsLoadedAcademicCollections() {
        Student student = Student.builder()
                .studentCode("20516041")
                .name("홍길동")
                .admissionYear(2020)
                .semesterEnrolled(10)
                .isTransferStudent(false)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(4)
                .completedSemesters(8)
                .admissionType("정시")
                .build();
        student.addSemesterRecord(new SemesterAcademicRecord(
                student,
                2024,
                1,
                18,
                18,
                BigDecimal.valueOf(4.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(4.0),
                1,
                100
        ));
        student.addStudentCourse(new StudentCourse(student, null, null, 3, false, null, false));

        student.clearAcademicAssociations();

        assertThat(student.getSemesterAcademicRecords()).isEmpty();
        assertThat(student.getStudentCourses()).isEmpty();
    }
}

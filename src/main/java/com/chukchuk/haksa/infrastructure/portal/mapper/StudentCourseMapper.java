package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.Student;

public class StudentCourseMapper {
    public static StudentCourse toEntity(CourseEnrollment enrollment, Student student, CourseOffering offering) {
        return new StudentCourse(
                student,
                offering,
                new Grade(enrollment.getGrade()),  // Embedded
                enrollment.getPoints(),
                enrollment.isRetake(),
                enrollment.getOriginalScore().intValue(),
                enrollment.isRetakeDeleted()
        );
    }
}
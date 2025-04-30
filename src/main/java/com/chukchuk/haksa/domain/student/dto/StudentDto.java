package com.chukchuk.haksa.domain.student.dto;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

public class StudentDto {

    public record StudentInfoDto(
            String studentCode,
            String name,
            String departmentName,
            String majorName,
            Integer gradeLevel,
            StudentStatus status,
            Integer completedSemesters,
            Instant updatedAt
    ) {
        public static StudentInfoDto from(Student student) {
            return new StudentInfoDto(
                    student.getStudentCode(),
                    student.getName(),
                    student.getDepartment() != null ? student.getDepartment().getEstablishedDepartmentName() : null,
                    student.getMajor() != null ? student.getMajor().getEstablishedDepartmentName() : null,
                    student.getAcademicInfo().getGradeLevel(),
                    student.getAcademicInfo().getStatus(),
                    student.getAcademicInfo().getCompletedSemesters(),
                    student.getUpdatedAt()
            );
        }
    }

    @Schema(description = "학생 프로필 정보")
    public record StudentProfileResponse(
            @Schema(description = "이름") String name,
            @Schema(description = "학번") String studentCode,
            @Schema(description = "학과 이름") String departmentName,
            @Schema(description = "전공 이름") String majorName,
            @Schema(description = "학년") int gradeLevel,
            @Schema(description = "현재 학기") int currentSemester,
            @Schema(description = "재학 상태") String status,
            @Schema(description = "마지막 업데이트 일시") String lastUpdatedAt,
            @Schema(description = "학사 정보 마지막 연동 일시") String lastSyncedAt
    ) {
        public static StudentProfileResponse from(StudentDto.StudentInfoDto studentInfoDto, int currentSemester, String lastSyncedAt) {
            return new StudentProfileResponse(
                    Objects.requireNonNullElse(studentInfoDto.name(), ""),
                    Objects.requireNonNullElse(studentInfoDto.studentCode(), ""),
                    Objects.requireNonNullElse(studentInfoDto.departmentName(), ""),
                    Objects.requireNonNullElse(studentInfoDto.majorName(), ""),
                    studentInfoDto.gradeLevel() != null ? studentInfoDto.gradeLevel() : 0,
                    currentSemester,
                    Objects.requireNonNullElse(studentInfoDto.status().toString(), ""),
                    studentInfoDto.updatedAt().toString(),
                    lastSyncedAt
            );
        }
    }


}

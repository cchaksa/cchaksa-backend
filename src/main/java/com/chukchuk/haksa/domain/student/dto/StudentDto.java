package com.chukchuk.haksa.domain.student.dto;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;

/** 학생 프로필 조회와 목표 학점 변경에 사용하는 요청·응답 형식을 묶는다. */
public class StudentDto {

  /**
   * 포털에서 동기화한 학생 학적 정보를 표현한다.
   *
   * @param studentCode 학번
   * @param name 이름
   * @param departmentName 학과 이름
   * @param majorName 전공 이름
   * @param dualMajorName 복수전공 이름
   * @param gradeLevel 학년
   * @param status 상태
   * @param completedSemesters 이수 학기 수
   * @param updatedAt 수정 시각
   * @param reconnectionRequired 포털 자격 증명을 다시 확인해야 하는지 여부
   */
  public record StudentInfoDto(
      String studentCode,
      String name,
      String departmentName,
      String majorName,
      String dualMajorName,
      Integer gradeLevel,
      StudentStatus status,
      Integer completedSemesters,
      Instant updatedAt,
      boolean reconnectionRequired) {
    /**
     * 학생 엔티티를 학생 정보 응답으로 변환한다.
     *
     * @param student 기록의 소유 학생
     * @return 학생 엔티티에서 변환한 학적 정보
     */
    public static StudentInfoDto from(Student student) {
      return new StudentInfoDto(
          student.getStudentCode(),
          student.getName(),
          student.getDepartment() != null
              ? student.getDepartment().getEstablishedDepartmentName()
              : null,
          student.getMajor() != null ? student.getMajor().getEstablishedDepartmentName() : null,
          student.getSecondaryMajor() != null
              ? student.getSecondaryMajor().getEstablishedDepartmentName()
              : null,
          student.getAcademicInfo().getGradeLevel(),
          student.getAcademicInfo().getStatus(),
          student.getAcademicInfo().getCompletedSemesters(),
          student.getUpdatedAt(),
          student.isReconnectionRequired());
    }
  }

  /**
   * 사용자 화면에 표시할 학생 프로필과 목표 학점을 표현한다.
   *
   * @param name 이름
   * @param studentCode 학번
   * @param departmentName 학과 이름
   * @param majorName 전공 이름
   * @param dualMajorName 복수전공 이름
   * @param gradeLevel 학년
   * @param currentSemester 현재 재학 학기 차수
   * @param status 상태
   * @param lastUpdatedAt last 수정 시각
   * @param lastSyncedAt 포털 학사 정보를 마지막으로 동기화한 시각
   * @param reconnectionRequired 포털 자격 증명을 다시 확인해야 하는지 여부
   */
  @Schema(description = "학생 프로필 정보")
  public record StudentProfileResponse(
      @Schema(description = "이름", required = true) String name,
      @Schema(description = "학번", required = true) String studentCode,
      @Schema(description = "학과 이름", required = true) String departmentName,
      @Schema(description = "전공 이름", required = true) String majorName,
      @Schema(description = "복수전공 이름", required = false) String dualMajorName,
      @Schema(description = "학년", required = true) int gradeLevel,
      @Schema(description = "현재 학기", required = true) int currentSemester,
      @Schema(description = "재학 상태", required = true, implementation = StudentStatus.class)
          StudentStatus status,
      @Schema(description = "마지막 업데이트 일시", required = true) String lastUpdatedAt,
      @Schema(description = "학사 정보 마지막 연동 일시", required = true) String lastSyncedAt,
      @Schema(description = "재연동 필요 여부", required = true) boolean reconnectionRequired) {
    /**
     * 학생 정보와 동기화 시각으로 학생 프로필 응답을 생성한다.
     *
     * @param studentInfoDto 프로필로 변환할 학생·학적 정보
     * @param currentSemester 현재 재학 학기 차수
     * @param lastSyncedAt 포털 학사 정보를 마지막으로 동기화한 시각
     * @return 학생 프로필 응답 결과
     */
    public static StudentProfileResponse from(
        StudentDto.StudentInfoDto studentInfoDto, int currentSemester, String lastSyncedAt) {
      return new StudentProfileResponse(
          Objects.requireNonNullElse(studentInfoDto.name(), ""),
          Objects.requireNonNullElse(studentInfoDto.studentCode(), ""),
          Objects.requireNonNullElse(studentInfoDto.departmentName(), ""),
          Objects.requireNonNullElse(studentInfoDto.majorName(), ""),
          Objects.requireNonNullElse(studentInfoDto.dualMajorName(), ""),
          studentInfoDto.gradeLevel() != null ? studentInfoDto.gradeLevel() : 0,
          currentSemester,
          studentInfoDto.status(),
          studentInfoDto.updatedAt() != null ? studentInfoDto.updatedAt().toString() : "",
          lastSyncedAt,
          studentInfoDto.reconnectionRequired());
    }
  }
}

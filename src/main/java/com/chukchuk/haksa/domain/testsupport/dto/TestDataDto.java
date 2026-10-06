// dev 테스트 어드민 API 요청과 응답 DTO를 정의한다

package com.chukchuk.haksa.domain.testsupport.dto;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/** 개발 환경의 관리자 테스트 API에서 사용하는 요청·응답 형식을 묶는다. */
public class TestDataDto {

  /**
   * 고정 테스트 계정 생성에 필요한 학적 조건을 전달한다.
   *
   * @param name 이름
   * @param departmentId 학과 식별자
   * @param majorId 전공 id 식별자
   * @param secondaryMajorDepartmentId secondary 전공 학과 식별자
   * @param admissionYear admission 연도
   * @param isPortalLinked 생성 직후 포털 연동 완료 상태로 둘지 여부
   */
  @Schema(description = "테스트 계정 생성 요청")
  public record CreateTestUserRequest(
      @Schema(description = "테스트 사용자 이름", example = "프론트테스트") String name,
      @Schema(description = "학과 ID", example = "1") Long departmentId,
      @Schema(description = "주전공 학과 ID", example = "1") Long majorId,
      @Schema(description = "복수전공 학과 ID", example = "2") Long secondaryMajorDepartmentId,
      @Schema(description = "입학년도", example = "2024") Integer admissionYear,
      @Schema(description = "포털 연동 여부. 비어 있으면 true로 처리합니다.", example = "true")
          Boolean isPortalLinked) {
    /**
     * 정규화된 검색 조건을 사용해 개설 강의 검색 요청을 생성한다.
     *
     * @param name 이름
     * @param departmentId 학과 식별자
     * @param majorId 전공 id 식별자
     * @param secondaryMajorDepartmentId secondary 전공 학과 식별자
     * @param admissionYear admission 연도
     */
    public CreateTestUserRequest(
        String name,
        Long departmentId,
        Long majorId,
        Long secondaryMajorDepartmentId,
        Integer admissionYear) {
      this(name, departmentId, majorId, secondaryMajorDepartmentId, admissionYear, null);
    }
  }

  /**
   * 생성된 테스트 계정과 인증 토큰을 전달한다.
   *
   * @param userId 사용자 식별자
   * @param studentId 학생 식별자
   * @param email 연락 및 로그인에 사용하는 이메일
   * @param studentCode 학번
   * @param accessToken 생성된 계정에 접근할 액세스 토큰
   * @param refreshToken refresh token 원문
   */
  @Schema(description = "테스트 계정 생성 응답")
  public record TestUserResponse(
      @Schema(description = "사용자 ID") UUID userId,
      @Schema(description = "학생 ID") UUID studentId,
      @Schema(description = "테스트 계정 이메일") String email,
      @Schema(description = "테스트 학번") String studentCode,
      @Schema(description = "Access Token") String accessToken,
      @Schema(description = "Refresh Token") String refreshToken) {}

  /**
   * 테스트 데이터 조작 화면에서 선택할 학과와 졸업 영역을 전달한다.
   *
   * @param departments 선택 가능한 학과 목록
   * @param graduationAreas 선택 가능한 졸업 요건 영역 목록
   */
  @Schema(description = "테스트 조작 옵션 응답")
  public record TestOptionsResponse(
      @Schema(description = "학과 목록") List<DepartmentOption> departments,
      @Schema(description = "졸업요건 영역 목록") List<GraduationAreaOption> graduationAreas) {}

  /**
   * 테스트 데이터 생성 화면에서 선택할 학과를 표현한다.
   *
   * @param id 학과 식별자
   * @param code 학과 코드
   * @param name 이름
   */
  @Schema(description = "학과 선택지")
  public record DepartmentOption(
      @Schema(description = "학과 ID") Long id,
      @Schema(description = "학과 코드") String code,
      @Schema(description = "학과명") String name) {}

  /**
   * 테스트 데이터 생성 화면에서 선택할 졸업 요건 영역을 표현한다.
   *
   * @param code 졸업 요건 영역 코드
   * @param name 이름
   */
  @Schema(description = "졸업요건 영역 선택지")
  public record GraduationAreaOption(
      @Schema(description = "영역 코드") String code, @Schema(description = "영역 표시명") String name) {}

  /**
   * 관리자 테스트용 개설 강의 검색 필터를 담는다.
   *
   * @param keyword 검색어
   * @param area 과목 영역 필터
   * @param year 연도
   * @param semester 대상 학기
   * @param departmentId 학과 식별자
   */
  @Schema(description = "강의 후보 검색 요청")
  public record CourseOfferingSearchRequest(
      @Schema(description = "과목명 또는 학수번호 검색어") String keyword,
      @Schema(description = "졸업요건 영역") FacultyDivision area,
      @Schema(description = "연도") Integer year,
      @Schema(description = "학기") Integer semester,
      @Schema(description = "학과 ID. 선교처럼 학과 필터가 필요 없는 영역은 생략합니다.") Long departmentId) {}

  /**
   * 강의평가 테스트 데이터에 연결할 개설 과목을 표현한다.
   *
   * @param offeringId 과목 개설 식별자
   * @param courseCode 과목 코드
   * @param courseName 과목 이름
   * @param year 연도
   * @param semester 대상 학기
   * @param credits 학점
   * @param area 과목 영역 필터
   * @param rawArea 포털에서 받은 원본 영역명
   * @param departmentName 학과 이름
   */
  @Schema(description = "강의 후보 선택지")
  public record CourseOfferingOption(
      @Schema(description = "개설강의 ID") Long offeringId,
      @Schema(description = "학수번호") String courseCode,
      @Schema(description = "과목명") String courseName,
      @Schema(description = "연도") Integer year,
      @Schema(description = "학기") Integer semester,
      @Schema(description = "학점") Integer credits,
      @Schema(description = "졸업요건 영역") FacultyDivision area,
      @Schema(description = "포털 원본 영역명") String rawArea,
      @Schema(description = "개설 학과명") String departmentName) {}

  /**
   * 테스트 계정의 졸업 영역별 수강 내역 변경 내용을 전달한다.
   *
   * @param area 과목 영역 필터
   * @param addOfferingIds 추가할 개설 과목 식별자 모음
   * @param removeStudentCourseIds 삭제할 학생 수강 내역 식별자 모음
   * @param grade 과목에서 취득한 성적
   * @param points 학점
   * @param isRetake 추가 과목을 재수강으로 기록할지 여부
   * @param originalScore 원점수
   */
  @Schema(description = "현재 인증 계정 강의 데이터 수정 요청")
  public record UpdateGraduationCoursesRequest(
      @Schema(description = "수정 대상 졸업요건 영역") FacultyDivision area,
      @Schema(description = "추가할 개설강의 ID 목록") List<Long> addOfferingIds,
      @Schema(description = "삭제할 학생 수강 row ID 목록") List<Long> removeStudentCourseIds,
      @Schema(description = "성적", example = "A+") String grade,
      @Schema(description = "학점", example = "3") Integer points,
      @Schema(description = "재수강 여부", example = "false") Boolean isRetake,
      @Schema(description = "원점수", example = "95") Integer originalScore) {}

  /**
   * 테스트 계정의 주전공과 복수전공 상태를 전달한다.
   *
   * @param majorDepartmentId 전공 학과 식별자
   * @param dualMajorEnabled 복수전공 사용 여부
   * @param secondaryMajorDepartmentId secondary 전공 학과 식별자
   */
  @Schema(description = "현재 인증 계정 전공 상태 수정 요청")
  public record UpdateMajorRequest(
      @Schema(description = "주전공 학과 ID") Long majorDepartmentId,
      @Schema(description = "복수전공 사용 여부") boolean dualMajorEnabled,
      @Schema(description = "복수전공 학과 ID") Long secondaryMajorDepartmentId) {}

  /**
   * 테스트 계정에 추가할 임의 수강 과목 정보를 전달한다.
   *
   * @param courseCode 과목 코드
   * @param courseName 과목 이름
   * @param area 과목 영역 필터
   * @param departmentId 학과 식별자
   * @param hostDepartment 주관 학과
   * @param year 연도
   * @param semester 대상 학기
   * @param credits 학점
   * @param grade 과목에서 취득한 성적
   * @param isRetake 생성 과목을 재수강으로 기록할지 여부
   * @param originalScore 원점수
   */
  @Schema(description = "현재 인증 계정 테스트 강의 생성 요청")
  public record CreateTestCourseRequest(
      @Schema(description = "테스트 학수번호. test_ prefix가 없으면 자동으로 붙습니다.") String courseCode,
      @Schema(description = "테스트 과목명") String courseName,
      @Schema(description = "졸업요건 영역") FacultyDivision area,
      @Schema(description = "학과 ID") Long departmentId,
      @Schema(description = "개설 학과명. departmentId가 없을 때 사용할 수 있습니다.") String hostDepartment,
      @Schema(description = "이수 연도") Integer year,
      @Schema(description = "학기") Integer semester,
      @Schema(description = "학점") Integer credits,
      @Schema(description = "성적") String grade,
      @Schema(description = "재수강 여부") Boolean isRetake,
      @Schema(description = "원점수") Integer originalScore) {}

  /**
   * 테스트 계정에 생성된 수강 과목 식별 정보를 전달한다.
   *
   * @param studentCourseId 학생 과목 식별자
   * @param offeringId 과목 개설 식별자
   * @param courseCode 과목 코드
   * @param courseName 과목 이름
   * @param area 과목 영역 필터
   */
  @Schema(description = "현재 인증 계정 테스트 강의 생성 응답")
  public record TestCourseResponse(
      @Schema(description = "생성된 학생 수강 row ID") Long studentCourseId,
      @Schema(description = "생성된 개설강의 ID") Long offeringId,
      @Schema(description = "생성된 학수번호") String courseCode,
      @Schema(description = "생성된 과목명") String courseName,
      @Schema(description = "졸업요건 영역") FacultyDivision area) {}
}

// dev 테스트 어드민 API 문서 인터페이스를 정의한다

package com.chukchuk.haksa.domain.admin.controller.docs;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.admin.dto.UpdateTransferDataRequest;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

/** 개발 환경에서 테스트 계정과 학사·강의평가 데이터를 구성하는 API 계약을 정의한다. */
@Tag(name = "Admin Test", description = "dev 전용 프론트 테스트 데이터 조작 API")
public interface AdminTestControllerDocs {

  /**
   * 개발 환경에서 요청 조건의 테스트 사용자와 인증 토큰을 생성한다.
   *
   * @param request 생성할 테스트 사용자의 학과·학적 조건
   * @return 생성된 테스트 사용자와 인증 토큰
   */
  @Operation(summary = "테스트 계정 생성", description = "dev 환경에서 테스트 계정을 생성하고 JWT 토큰을 발급합니다.")
  ResponseEntity<SuccessResponse<AdminTestDto.TestUserResponse>> createTestUser(
      AdminTestDto.CreateTestUserRequest request);

  /**
   * 테스트 데이터 구성에 사용할 기본 선택지를 반환한다.
   *
   * @return 학과와 졸업 영역 등 테스트 구성 선택지
   */
  @Operation(summary = "테스트 조작 옵션 조회", description = "dev 환경에서 토큰 없이 학과와 졸업요건 영역 선택지를 조회합니다.")
  ResponseEntity<SuccessResponse<AdminTestDto.TestOptionsResponse>> getTestOptions();

  /**
   * 학과 코드 또는 이름에 검색어가 포함된 학과를 반환한다.
   *
   * @param keyword 검색할 과목 코드 또는 이름
   * @return 조건에 일치하는 개발 환경 테스트 데이터 목록
   */
  @Operation(summary = "학과 검색", description = "dev 환경에서 토큰 없이 학과 코드와 학과명으로 학과 선택지를 검색합니다.")
  ResponseEntity<SuccessResponse<List<AdminTestDto.DepartmentOption>>> searchDepartments(
      String keyword);

  /**
   * 선택 필터에 맞는 개설 강의 후보를 반환한다.
   *
   * @param keyword 검색할 과목 코드 또는 이름
   * @param area 과목 영역 필터
   * @param year 대상 연도
   * @param semester 대상 학기
   * @param departmentId 학과 식별자
   * @return 조건에 일치하는 개발 환경 테스트 데이터 목록
   */
  @Operation(summary = "강의 후보 조회", description = "dev 환경에서 토큰 없이 테스트 데이터에 추가할 개설강의 후보를 검색합니다.")
  ResponseEntity<SuccessResponse<List<AdminTestDto.CourseOfferingOption>>> searchCourseOfferings(
      String keyword,
      com.chukchuk.haksa.domain.course.model.FacultyDivision area,
      Integer year,
      Integer semester,
      Long departmentId);

  /**
   * 테스트 계정의 졸업 판정용 수강 과목을 요청 목록으로 교체한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @param request 추가·삭제할 졸업 판정용 수강 과목 목록
   * @return 수강 과목 변경 완료 메시지를 담은 성공 응답
   */
  @Operation(summary = "현재 계정 강의 데이터 수정", description = "현재 인증 계정의 졸업요건 강의 데이터를 추가하거나 삭제합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateGraduationCourses(
      CustomUserDetails userDetails, AdminTestDto.UpdateGraduationCoursesRequest request);

  /**
   * 테스트 계정의 주전공과 복수전공을 요청 값으로 변경한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @param request 변경할 주전공과 복수전공 식별정보
   * @return 전공 변경 완료 메시지를 담은 성공 응답
   */
  @Operation(summary = "현재 계정 전공 상태 수정", description = "현재 인증 계정의 주전공과 복수전공 상태를 수정합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateMajor(
      CustomUserDetails userDetails, AdminTestDto.UpdateMajorRequest request);

  /**
   * 인증된 테스트 계정의 편입 진단용 원천 데이터를 부분 수정한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @param request 생략한 값은 유지할 편입 원천 데이터
   * @return 편입 데이터 변경 완료 메시지
   */
  @Operation(
      summary = "테스트 계정 편입 데이터 수정",
      description =
          "dev/test에서 생성한 본인 테스트 계정만 수정합니다. 생략은 유지하고 명시적 null은 400입니다. "
              + "false, 0과 빈 지정과목 배열은 반영합니다. 결과는 기존 GET /api/graduation/progress로 확인합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(responseCode = "200", description = "편입 테스트 데이터 수정 완료")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류(C01) 또는 학생 미연결(U04)",
      content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
  @ApiResponse(
      responseCode = "403",
      description = "테스트 계정이 아님(C04)",
      content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateTransferData(
      CustomUserDetails userDetails, UpdateTransferDataRequest request);

  /**
   * 현재 관리자 테스트 계정의 학사 데이터를 초기화한다.
   *
   * @param userDetails 사용자 상세 정보
   * @return 테스트 학사 데이터 초기화 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "현재 계정 테스트 데이터 초기화",
      description =
          "현재 인증 계정의 수강 데이터와 전공 상태를 초기화합니다. 테스트 계정은 추가로 편입 여부(false), "
              + "입학 구분(신입), 이수 학기(0)를 복원하고 누적 성적·외국어 인증·지정과목 및 수신 상태를 초기화합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetCurrentAccount(
      CustomUserDetails userDetails);

  /**
   * 테스트용 과목과 개설 강의를 만들고 학생의 수강 내역에 추가한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @param request 학생 수강 내역에 추가할 테스트 과목 정보
   * @return 생성되어 수강 내역에 추가된 테스트 과목
   */
  @Operation(
      summary = "현재 계정 테스트 강의 생성",
      description = "테스트 강의와 개설강의를 만들고 현재 인증 계정의 수강 데이터에 바로 추가합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<AdminTestDto.TestCourseResponse>> createTestCourse(
      CustomUserDetails userDetails, AdminTestDto.CreateTestCourseRequest request);

  /**
   * 고정 테스트 계정의 대상 학기 평가·수강·성적 기록을 제거한다.
   *
   * @return 대상 학기 데이터 삭제 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "dev 강의평가 empty-semester 상태 세팅",
      description = "고정 프론트 테스트 계정의 target 학기 평가/수강/학기 row를 삭제합니다.")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationEmptySemester();

  /**
   * 고정 테스트 계정의 대상 학기를 성적 미공개 상태로 재구성한다.
   *
   * @return 성적 미공개 상태 구성 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "dev 강의평가 NOT_RELEASED 상태 세팅",
      description = "고정 프론트 테스트 계정의 target 학기를 성적 미공개 상태로 재구성합니다.")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationNotReleased();

  /**
   * 고정 테스트 계정의 대상 학기를 강의평가 제출 대기 상태로 재구성한다.
   *
   * @return 강의평가 대기 상태 구성 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "dev 강의평가 PENDING 상태 세팅",
      description = "고정 프론트 테스트 계정의 target 학기를 강의평가 대기 상태로 재구성합니다.")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationPending();

  /**
   * 고정 테스트 계정의 대상 학기를 강의평가 건너뜀 상태로 재구성한다.
   *
   * @return 강의평가 건너뜀 상태 구성 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "dev 강의평가 SKIPPED 상태 세팅",
      description = "고정 프론트 테스트 계정의 target 학기를 강의평가 건너뛰기 상태로 재구성합니다.")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationSkipped();

  /**
   * 고정 테스트 계정의 대상 학기를 강의평가 제출 완료 상태로 재구성한다.
   *
   * @return 강의평가 완료 상태 구성 완료 메시지를 담은 성공 응답
   */
  @Operation(
      summary = "dev 강의평가 COMPLETED 상태 세팅",
      description = "고정 프론트 테스트 계정의 target 학기를 강의평가 완료 상태로 재구성합니다.")
  ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationCompleted();
}

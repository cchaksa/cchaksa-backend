// dev 테스트 어드민 API 문서 인터페이스를 정의한다
package com.chukchuk.haksa.domain.admin.controller.docs;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Admin Test", description = "dev 전용 프론트 테스트 데이터 조작 API")
public interface AdminTestControllerDocs {

    @Operation(summary = "테스트 계정 생성", description = "dev 환경에서 테스트 계정을 생성하고 JWT 토큰을 발급합니다.")
    ResponseEntity<SuccessResponse<AdminTestDto.TestUserResponse>> createTestUser(AdminTestDto.CreateTestUserRequest request);

    @Operation(summary = "테스트 조작 옵션 조회", description = "dev 환경에서 토큰 없이 학과와 졸업요건 영역 선택지를 조회합니다.")
    ResponseEntity<SuccessResponse<AdminTestDto.TestOptionsResponse>> getTestOptions();

    @Operation(summary = "학과 검색", description = "dev 환경에서 토큰 없이 학과 코드와 학과명으로 학과 선택지를 검색합니다.")
    ResponseEntity<SuccessResponse<List<AdminTestDto.DepartmentOption>>> searchDepartments(String keyword);

    @Operation(summary = "강의 후보 조회", description = "dev 환경에서 토큰 없이 테스트 데이터에 추가할 개설강의 후보를 검색합니다.")
    ResponseEntity<SuccessResponse<List<AdminTestDto.CourseOfferingOption>>> searchCourseOfferings(
            String keyword,
            com.chukchuk.haksa.domain.course.model.FacultyDivision area,
            Integer year,
            Integer semester,
            Long departmentId
    );

    @Operation(summary = "졸업요건 누락 진단", description = "dev 환경에서 학번 기준 졸업요건 데이터 존재 여부를 진단합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<AdminTestDto.GraduationRequirementDiagnosticResponse>> diagnoseGraduationRequirements(
            String studentCode
    );

    @Operation(summary = "누락 졸업요건 생성", description = "dev 환경에서 PDF 추출 리소스를 기준으로 학생의 누락 졸업요건을 dry-run 또는 실제 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<AdminTestDto.CreateMissingGraduationRequirementsResponse>> createMissingGraduationRequirements(
            AdminTestDto.CreateMissingGraduationRequirementsRequest request
    );

    @Operation(summary = "현재 계정 강의 데이터 수정", description = "현재 인증 계정의 졸업요건 강의 데이터를 추가하거나 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateGraduationCourses(
            CustomUserDetails userDetails,
            AdminTestDto.UpdateGraduationCoursesRequest request
    );

    @Operation(summary = "현재 계정 전공 상태 수정", description = "현재 인증 계정의 주전공과 복수전공 상태를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateMajor(
            CustomUserDetails userDetails,
            AdminTestDto.UpdateMajorRequest request
    );

    @Operation(summary = "현재 계정 테스트 데이터 초기화", description = "현재 인증 계정의 수강 데이터와 전공 상태를 프론트 테스트 기준 상태로 초기화합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetCurrentAccount(CustomUserDetails userDetails);

    @Operation(summary = "현재 계정 테스트 강의 생성", description = "테스트 강의와 개설강의를 만들고 현재 인증 계정의 수강 데이터에 바로 추가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<AdminTestDto.TestCourseResponse>> createTestCourse(
            CustomUserDetails userDetails,
            AdminTestDto.CreateTestCourseRequest request
    );

    @Operation(summary = "dev 강의평가 empty-semester 상태 세팅", description = "고정 프론트 테스트 계정의 target 학기 평가/수강/학기 row를 삭제합니다.")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationEmptySemester();

    @Operation(summary = "dev 강의평가 NOT_RELEASED 상태 세팅", description = "고정 프론트 테스트 계정의 target 학기를 성적 미공개 상태로 재구성합니다.")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationNotReleased();

    @Operation(summary = "dev 강의평가 PENDING 상태 세팅", description = "고정 프론트 테스트 계정의 target 학기를 강의평가 대기 상태로 재구성합니다.")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationPending();

    @Operation(summary = "dev 강의평가 SKIPPED 상태 세팅", description = "고정 프론트 테스트 계정의 target 학기를 강의평가 건너뛰기 상태로 재구성합니다.")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationSkipped();

    @Operation(summary = "dev 강의평가 COMPLETED 상태 세팅", description = "고정 프론트 테스트 계정의 target 학기를 강의평가 완료 상태로 재구성합니다.")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationCompleted();
}

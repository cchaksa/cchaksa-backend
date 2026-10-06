// dev/test 환경의 테스트 데이터 API 엔드포인트를 제공한다

package com.chukchuk.haksa.domain.testsupport.controller;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.testsupport.controller.docs.TestDataControllerDocs;
import com.chukchuk.haksa.domain.testsupport.dto.TestDataDto;
import com.chukchuk.haksa.domain.testsupport.dto.UpdateTransferDataRequest;
import com.chukchuk.haksa.domain.testsupport.service.TestAccountService;
import com.chukchuk.haksa.domain.testsupport.service.TestLectureEvaluationService;
import com.chukchuk.haksa.domain.testsupport.service.TestMutationService;
import com.chukchuk.haksa.domain.testsupport.service.TestOptionService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 개발 환경의 테스트 계정과 학사·강의평가 데이터 조작 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
@Profile({"dev", "test"})
@RequestMapping("/api/test")
public class TestDataController implements TestDataControllerDocs {

  private final TestAccountService accountService;
  private final TestOptionService optionService;
  private final TestMutationService mutationService;
  private final TestLectureEvaluationService lectureEvaluationService;

  @Override
  @PostMapping("/users")
  public ResponseEntity<SuccessResponse<TestDataDto.TestUserResponse>> createTestUser(
      @Valid @RequestBody TestDataDto.CreateTestUserRequest request) {
    return ResponseEntity.ok(SuccessResponse.of(accountService.createTestUser(request)));
  }

  @Override
  @GetMapping("/options")
  public ResponseEntity<SuccessResponse<TestDataDto.TestOptionsResponse>> getTestOptions() {
    return ResponseEntity.ok(SuccessResponse.of(optionService.getTestOptions()));
  }

  @Override
  @GetMapping("/departments")
  public ResponseEntity<SuccessResponse<List<TestDataDto.DepartmentOption>>> searchDepartments(
      @RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(SuccessResponse.of(optionService.searchDepartments(keyword)));
  }

  @Override
  @GetMapping("/course-offerings")
  public ResponseEntity<SuccessResponse<List<TestDataDto.CourseOfferingOption>>>
      searchCourseOfferings(
          @RequestParam(required = false) String keyword,
          @RequestParam(required = false) FacultyDivision area,
          @RequestParam(required = false) Integer year,
          @RequestParam(required = false) Integer semester,
          @RequestParam(required = false) Long departmentId) {
    TestDataDto.CourseOfferingSearchRequest request =
        new TestDataDto.CourseOfferingSearchRequest(keyword, area, year, semester, departmentId);
    return ResponseEntity.ok(SuccessResponse.of(optionService.searchCourseOfferings(request)));
  }

  @Override
  @PatchMapping("/me/graduation-courses")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateGraduationCourses(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TestDataDto.UpdateGraduationCoursesRequest request) {
    mutationService.updateGraduationCourses(userDetails.getId(), request);
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("강의 데이터가 수정되었습니다.")));
  }

  @Override
  @PatchMapping("/me/major")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateMajor(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TestDataDto.UpdateMajorRequest request) {
    mutationService.updateMajor(userDetails.getId(), request);
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("전공 상태가 수정되었습니다.")));
  }

  @Override
  @PatchMapping("/me/transfer-data")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateTransferData(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateTransferDataRequest request) {
    mutationService.updateTransferData(userDetails.getId(), request);
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("편입 테스트 데이터가 수정되었습니다.")));
  }

  @Override
  @PostMapping("/me/reset")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetCurrentAccount(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    mutationService.resetCurrentAccount(userDetails.getId());
    return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("테스트 데이터가 초기화되었습니다.")));
  }

  @Override
  @PostMapping("/me/test-courses")
  public ResponseEntity<SuccessResponse<TestDataDto.TestCourseResponse>> createTestCourse(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody TestDataDto.CreateTestCourseRequest request) {
    return ResponseEntity.ok(
        SuccessResponse.of(mutationService.createTestCourse(userDetails.getId(), request)));
  }

  @Override
  @PostMapping("/lecture-evaluations/empty-semester")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationEmptySemester() {
    lectureEvaluationService.setEmptySemester();
    return ResponseEntity.ok(
        SuccessResponse.of(new MessageOnlyResponse("강의평가 테스트 상태가 empty-semester로 변경되었습니다.")));
  }

  @Override
  @PostMapping("/lecture-evaluations/not-released")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationNotReleased() {
    lectureEvaluationService.setNotReleased();
    return ResponseEntity.ok(
        SuccessResponse.of(new MessageOnlyResponse("강의평가 테스트 상태가 NOT_RELEASED로 변경되었습니다.")));
  }

  @Override
  @PostMapping("/lecture-evaluations/pending")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationPending() {
    lectureEvaluationService.setPending();
    return ResponseEntity.ok(
        SuccessResponse.of(new MessageOnlyResponse("강의평가 테스트 상태가 PENDING으로 변경되었습니다.")));
  }

  @Override
  @PostMapping("/lecture-evaluations/skipped")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationSkipped() {
    lectureEvaluationService.setSkipped();
    return ResponseEntity.ok(
        SuccessResponse.of(new MessageOnlyResponse("강의평가 테스트 상태가 SKIPPED로 변경되었습니다.")));
  }

  @Override
  @PostMapping("/lecture-evaluations/completed")
  public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setLectureEvaluationCompleted() {
    lectureEvaluationService.setCompleted();
    return ResponseEntity.ok(
        SuccessResponse.of(new MessageOnlyResponse("강의평가 테스트 상태가 COMPLETED로 변경되었습니다.")));
  }
}

// dev 테스트 어드민 API 엔드포인트를 제공한다
package com.chukchuk.haksa.domain.admin.controller;

import com.chukchuk.haksa.domain.admin.controller.docs.AdminTestControllerDocs;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.admin.service.AdminTestAccountService;
import com.chukchuk.haksa.domain.admin.service.AdminTestMutationService;
import com.chukchuk.haksa.domain.admin.service.AdminTestOptionService;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile({"dev", "test"})
@RequestMapping("/api/admin")
public class AdminTestController implements AdminTestControllerDocs {

    private final AdminTestAccountService accountService;
    private final AdminTestOptionService optionService;
    private final AdminTestMutationService mutationService;

    @Override
    @PostMapping("/test-users")
    public ResponseEntity<SuccessResponse<AdminTestDto.TestUserResponse>> createTestUser(
            @Valid @RequestBody AdminTestDto.CreateTestUserRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.of(accountService.createTestUser(request)));
    }

    @Override
    @GetMapping("/test-options")
    public ResponseEntity<SuccessResponse<AdminTestDto.TestOptionsResponse>> getTestOptions() {
        return ResponseEntity.ok(SuccessResponse.of(optionService.getTestOptions()));
    }

    @Override
    @GetMapping("/departments")
    public ResponseEntity<SuccessResponse<List<AdminTestDto.DepartmentOption>>> searchDepartments(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(SuccessResponse.of(optionService.searchDepartments(keyword)));
    }

    @Override
    @GetMapping("/course-offerings")
    public ResponseEntity<SuccessResponse<List<AdminTestDto.CourseOfferingOption>>> searchCourseOfferings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) FacultyDivision area,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Long departmentId
    ) {
        AdminTestDto.CourseOfferingSearchRequest request = new AdminTestDto.CourseOfferingSearchRequest(
                keyword,
                area,
                year,
                semester,
                departmentId
        );
        return ResponseEntity.ok(SuccessResponse.of(optionService.searchCourseOfferings(request)));
    }

    @Override
    @PatchMapping("/me/graduation-courses")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateGraduationCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AdminTestDto.UpdateGraduationCoursesRequest request
    ) {
        mutationService.updateGraduationCourses(userDetails.getId(), request);
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("강의 데이터가 수정되었습니다.")));
    }

    @Override
    @PatchMapping("/me/major")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> updateMajor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AdminTestDto.UpdateMajorRequest request
    ) {
        mutationService.updateMajor(userDetails.getId(), request);
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("전공 상태가 수정되었습니다.")));
    }
}

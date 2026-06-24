// dev 테스트 어드민 API 요청과 응답 DTO를 정의한다
package com.chukchuk.haksa.domain.admin.dto;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public class AdminTestDto {

    @Schema(description = "테스트 계정 생성 요청")
    public record CreateTestUserRequest(
            @Schema(description = "테스트 사용자 이름", example = "프론트테스트") String name,
            @Schema(description = "학과 ID", example = "1") Long departmentId,
            @Schema(description = "주전공 학과 ID", example = "1") Long majorId,
            @Schema(description = "복수전공 학과 ID", example = "2") Long secondaryMajorDepartmentId,
            @Schema(description = "입학년도", example = "2024") Integer admissionYear
    ) {
    }

    @Schema(description = "테스트 계정 생성 응답")
    public record TestUserResponse(
            @Schema(description = "사용자 ID") UUID userId,
            @Schema(description = "학생 ID") UUID studentId,
            @Schema(description = "테스트 계정 이메일") String email,
            @Schema(description = "테스트 학번") String studentCode,
            @Schema(description = "Access Token") String accessToken,
            @Schema(description = "Refresh Token") String refreshToken
    ) {
    }

    @Schema(description = "테스트 조작 옵션 응답")
    public record TestOptionsResponse(
            @Schema(description = "학과 목록") List<DepartmentOption> departments,
            @Schema(description = "졸업요건 영역 목록") List<GraduationAreaOption> graduationAreas
    ) {
    }

    @Schema(description = "학과 선택지")
    public record DepartmentOption(
            @Schema(description = "학과 ID") Long id,
            @Schema(description = "학과 코드") String code,
            @Schema(description = "학과명") String name
    ) {
    }

    @Schema(description = "졸업요건 영역 선택지")
    public record GraduationAreaOption(
            @Schema(description = "영역 코드") String code,
            @Schema(description = "영역 표시명") String name
    ) {
    }

    @Schema(description = "강의 후보 검색 요청")
    public record CourseOfferingSearchRequest(
            @Schema(description = "과목명 또는 학수번호 검색어") String keyword,
            @Schema(description = "졸업요건 영역") FacultyDivision area,
            @Schema(description = "연도") Integer year,
            @Schema(description = "학기") Integer semester,
            @Schema(description = "학과 ID. 선교처럼 학과 필터가 필요 없는 영역은 생략합니다.") Long departmentId
    ) {
    }

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
            @Schema(description = "개설 학과명") String departmentName
    ) {
    }

    @Schema(description = "현재 인증 계정 강의 데이터 수정 요청")
    public record UpdateGraduationCoursesRequest(
            @Schema(description = "수정 대상 졸업요건 영역") FacultyDivision area,
            @Schema(description = "추가할 개설강의 ID 목록") List<Long> addOfferingIds,
            @Schema(description = "삭제할 학생 수강 row ID 목록") List<Long> removeStudentCourseIds,
            @Schema(description = "성적", example = "A+") String grade,
            @Schema(description = "학점", example = "3") Integer points,
            @Schema(description = "재수강 여부", example = "false") Boolean isRetake,
            @Schema(description = "원점수", example = "95") Integer originalScore
    ) {
    }

    @Schema(description = "현재 인증 계정 전공 상태 수정 요청")
    public record UpdateMajorRequest(
            @Schema(description = "주전공 학과 ID") Long majorDepartmentId,
            @Schema(description = "복수전공 사용 여부") boolean dualMajorEnabled,
            @Schema(description = "복수전공 학과 ID") Long secondaryMajorDepartmentId
    ) {
    }
}

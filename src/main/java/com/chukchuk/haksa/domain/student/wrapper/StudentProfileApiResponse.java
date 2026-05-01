package com.chukchuk.haksa.domain.student.wrapper;

import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.chukchuk.haksa.domain.student.dto.StudentDto.StudentProfileResponse;

@Schema(name = "StudentProfileApiResponse", description = "학생 프로필 응답")
public class StudentProfileApiResponse extends SuccessResponse<StudentProfileResponse> {

    public StudentProfileApiResponse() {
        super(new StudentProfileResponse(
                "홍길동",                 // name
                "20231234",               // studentCode
                "컴퓨터공학과",           // departmentName
                "컴퓨터SW학과",           // majorName
                "경영학과",               // dualMajorName
                3,                        // gradeLevel
                2,                        // currentSemester
                StudentStatus.재학,       // status
                "2026-04-16T12:00:00Z",   // lastUpdatedAt
                "2026-04-16T11:30:00Z",   // lastSyncedAt
                false
        ), "요청 성공");
    }
}

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
                3,                        // gradeLevel
                6,                        // currentSemester
                StudentStatus.재학,               // status
                "2024-04-25T10:00:00Z",    // lastUpdatedAt
                "2024-04-25T08:00:00Z"     // lastSyncedAt
        ), "요청 성공");
    }
}
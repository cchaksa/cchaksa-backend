package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "졸업 요건 진행 상황 응답")
public class GraduationProgressResponse {
    @Schema(description = "졸업 요건 영역별 이수 현황", required = true)
    private List<AreaProgressDto> graduationProgress;

    @Schema(description = "특정 학과/연도 예외로 기존과 다른 졸업요건이 적용되는지 여부", required = true)
    private boolean hasDifferentGraduationRequirement = false;

    public GraduationProgressResponse(List<AreaProgressDto> graduationProgress) {
        this.graduationProgress = graduationProgress;
    }

    public void setHasDifferentGraduationRequirement() {
        this.hasDifferentGraduationRequirement = true;
    }
}

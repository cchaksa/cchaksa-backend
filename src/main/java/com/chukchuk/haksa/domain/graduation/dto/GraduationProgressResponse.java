package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "졸업 요건 진행 상황 응답")
public class GraduationProgressResponse {
    @Schema(description = "졸업 요건 영역별 이수 현황", required = true)
    private List<AreaProgressDto> graduationProgress;

    @Schema(description = "외국어 졸업 인증 통과 여부. 새 크롤러 동기화 전이면 null", nullable = true)
    private Boolean languageCertFulfilled;

    @Schema(description = "외국어 졸업 인증 정보를 확인하려면 포털 새로고침이 필요한지 여부", required = true)
    private boolean languageCertNeedsRefresh;

    @Schema(description = "특정 학과/연도 예외로 기존과 다른 졸업요건이 적용되는지 여부", required = true)
    private boolean hasDifferentGraduationRequirement = false;

    public GraduationProgressResponse(List<AreaProgressDto> graduationProgress) {
        this(graduationProgress, null);
    }

    public GraduationProgressResponse(
            List<AreaProgressDto> graduationProgress,
            Boolean languageCertFulfilled
    ) {
        this.graduationProgress = graduationProgress;
        this.languageCertFulfilled = languageCertFulfilled;
        this.languageCertNeedsRefresh = languageCertFulfilled == null;
    }

    public void setHasDifferentGraduationRequirement() {
        this.hasDifferentGraduationRequirement = true;
    }
}

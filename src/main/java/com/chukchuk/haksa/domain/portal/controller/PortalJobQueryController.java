package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLinkJobQueryService;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/link/jobs")
@RequiredArgsConstructor
public class PortalJobQueryController {

    private final PortalLinkJobQueryService portalLinkJobQueryService;

    @GetMapping("/{jobId}")
    public ResponseEntity<SuccessResponse<PortalLinkDto.JobStatusResponse>> getJobStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String jobId
    ) {
        PortalLinkDto.JobStatusResponse response = portalLinkJobQueryService.getJobStatus(userDetails.getId(), jobId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}

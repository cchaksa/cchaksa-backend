package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLinkJobService;
import com.chukchuk.haksa.domain.portal.controller.docs.PortalLinkCommandControllerDocs;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/link")
@RequiredArgsConstructor
public class PortalLinkController implements PortalLinkCommandControllerDocs {

    private final PortalLinkJobService portalLinkJobService;

    @PostMapping
    public ResponseEntity<SuccessResponse<PortalLinkDto.AcceptedResponse>> createPortalLinkJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PortalLinkDto.LinkRequest request
    ) {
        PortalLinkDto.AcceptedResponse response = portalLinkJobService.acceptJob(userDetails.getId(), idempotencyKey, request);
        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }
}

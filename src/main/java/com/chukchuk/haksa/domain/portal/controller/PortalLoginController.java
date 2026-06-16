// 포털 로그인 검증 요청을 처리하는 컨트롤러
package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLoginService;
import com.chukchuk.haksa.domain.portal.controller.docs.PortalLoginControllerDocs;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalLoginController implements PortalLoginControllerDocs {

    private final PortalLoginService portalLoginService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<PortalLinkDto.LoginResponse>> verifyPortalLogin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PortalLinkDto.LoginRequest request
    ) {
        PortalLinkDto.LoginResponse response = portalLoginService.login(userDetails.getId(), request);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}

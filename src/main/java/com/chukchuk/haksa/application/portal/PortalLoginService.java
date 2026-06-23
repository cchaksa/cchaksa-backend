// 포털 로그인 검증 후 연동용 검증 token을 발급하는 서비스
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalLoginService {

    private final PortalLoginVerifier portalLoginVerifier;
    private final PortalLoginVerificationTokenService tokenService;

    public PortalLinkDto.LoginResponse login(UUID userId, PortalLinkDto.LoginRequest request) {
        validateRequest(request);
        portalLoginVerifier.verify(request.portal_type(), request.username(), request.password());
        String token = tokenService.issue(userId, request.portal_type(), request.username(), request.password());
        return new PortalLinkDto.LoginResponse(token);
    }

    private void validateRequest(PortalLinkDto.LoginRequest request) {
        if (request.username() == null || request.username().isBlank() || request.password() == null || request.password().isBlank()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        if (!"suwon".equals(normalize(request.portal_type()))) {
            throw new CommonException(ErrorCode.UNSUPPORTED_PORTAL_TYPE);
        }
    }

    private String normalize(String portalType) {
        return portalType == null ? "" : portalType.trim().toLowerCase();
    }
}

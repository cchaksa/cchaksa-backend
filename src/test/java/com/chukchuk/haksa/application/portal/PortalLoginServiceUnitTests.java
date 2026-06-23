// 포털 로그인 검증 서비스의 token 발급 흐름을 검증하는 테스트
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLoginServiceUnitTests {

    @Mock
    private PortalLoginVerifier portalLoginVerifier;

    @Mock
    private PortalLoginVerificationTokenService tokenService;

    @Test
    @DisplayName("포털 로그인 검증에 성공하면 verification token을 발급한다")
    void login_issuesVerificationTokenAfterPortalLoginSuccess() {
        UUID userId = UUID.randomUUID();
        PortalLoginService service = new PortalLoginService(portalLoginVerifier, tokenService);
        PortalLinkDto.LoginRequest request = new PortalLinkDto.LoginRequest("suwon", "17019013", "pw");
        when(tokenService.issue(userId, "suwon", "17019013", "pw")).thenReturn("verification-token");

        PortalLinkDto.LoginResponse response = service.login(userId, request);

        assertThat(response.portal_verification_token()).isEqualTo("verification-token");
        InOrder inOrder = inOrder(portalLoginVerifier, tokenService);
        inOrder.verify(portalLoginVerifier).verify("suwon", "17019013", "pw");
        inOrder.verify(tokenService).issue(userId, "suwon", "17019013", "pw");
    }

    @Test
    @DisplayName("포털 로그인 검증에 실패하면 verification token을 발급하지 않는다")
    void login_doesNotIssueVerificationTokenWhenPortalLoginFails() {
        UUID userId = UUID.randomUUID();
        PortalLoginService service = new PortalLoginService(portalLoginVerifier, tokenService);
        PortalLinkDto.LoginRequest request = new PortalLinkDto.LoginRequest("suwon", "17019013", "wrong");
        doThrow(new CommonException(ErrorCode.PORTAL_LOGIN_FAILED))
                .when(portalLoginVerifier).verify("suwon", "17019013", "wrong");

        assertThatThrownBy(() -> service.login(userId, request))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.PORTAL_LOGIN_FAILED.code()));
        verify(tokenService, never()).issue(userId, "suwon", "17019013", "wrong");
    }
}

package com.chukchuk.haksa.domain.auth.controller;

import com.chukchuk.haksa.domain.auth.dto.AuthDto.CsrfTokenResponse;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.domain.auth.service.TokenCookieProvider;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private TokenCookieProvider tokenCookieProvider;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(refreshTokenService, tokenCookieProvider);
    }

    @Test
    void returnsCsrfTokenResponse() {
        CsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "sample-token");

        ResponseEntity<SuccessResponse<CsrfTokenResponse>> response = authController.getCsrfToken(csrfToken);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().token()).isEqualTo("sample-token");
        assertThat(response.getBody().getData().headerName()).isEqualTo("X-XSRF-TOKEN");
    }
}

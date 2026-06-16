// 포털 로그인 검증 API의 HTTP 계약을 검증하는 테스트
package com.chukchuk.haksa.domain.portal.controller;

import com.chukchuk.haksa.application.portal.PortalLoginService;
import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalLoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalLoginControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalLoginService portalLoginService;

    @Test
    @DisplayName("포털 로그인 검증 성공 시 verification token을 반환한다")
    void verifyPortalLogin_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(portalLoginService.login(eq(userId), any(PortalLinkDto.LoginRequest.class)))
                .thenReturn(new PortalLinkDto.LoginResponse("verification-token"));

        mockMvc.perform(post("/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portal_type":"suwon",
                                  "username":"17019013",
                                  "password":"pw"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.portal_verification_token").value("verification-token"));
    }
}

package com.chukchuk.haksa.domain.user.controller;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("signin 성공 시 성공 응답을 반환한다")
    void signIn_success() throws Exception {
        when(userService.signIn(any()))
                .thenReturn(new AuthDto.SignInTokenResponse("access-token", "refresh-token", false));

        mockMvc.perform(post("/api/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token":"dummy-id-token",
                                  "nonce":"dummy-nonce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.isPortalLinked").value(false));
    }

    @Test
    @DisplayName("delete 성공 시 성공 응답을 반환한다")
    void delete_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());
        doNothing().when(userService).deleteUserById(userId);

        mockMvc.perform(delete("/api/users/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("signin 토큰 검증 실패 시 에러 응답을 반환한다")
    void signIn_tokenInvalid() throws Exception {
        when(userService.signIn(any()))
                .thenThrow(new TokenException(ErrorCode.TOKEN_INVALID));

        mockMvc.perform(post("/api/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_token":"invalid-id-token",
                                  "nonce":"dummy-nonce"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("T10"));
    }
}

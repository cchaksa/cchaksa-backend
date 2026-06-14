// JWT 인증 필터의 invalid access token 401 응답을 검증하는 테스트
package com.chukchuk.haksa.global.security.filter;

import com.chukchuk.haksa.domain.student.controller.StudentController;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.controller.UserController;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.security.SecurityConfig;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.handler.CustomAccessDeniedHandler;
import com.chukchuk.haksa.global.security.handler.CustomAuthenticationEntryPoint;
import com.chukchuk.haksa.global.security.service.CustomUserDetailsService;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, StudentController.class})
@AutoConfigureMockMvc(addFilters = true)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        JwtAuthenticationFilterTests.TestCorsConfig.class
})
class JwtAuthenticationFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private AuthTokenCache authTokenCache;

    @MockBean
    private UserService userService;

    @MockBean
    private StudentService studentService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("만료된 acToken으로 /api/users/me 호출 시 401 TOKEN_EXPIRED를 반환한다")
    void getMe_withExpiredAccessToken_returns401() throws Exception {
        String token = "expired-access-token";
        when(jwtProvider.parseToken(token))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TOKEN_EXPIRED.code()))
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"TOKEN_EXPIRED\""));
    }

    @Test
    @DisplayName("유효하지 않은 acToken으로 /api/student/profile 호출 시 401 TOKEN_INVALID를 반환한다")
    void getStudentProfile_withInvalidAccessToken_returns401() throws Exception {
        String token = "invalid-access-token";
        when(jwtProvider.parseToken(token))
                .thenThrow(new JwtException("invalid"));

        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TOKEN_INVALID.code()))
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"TOKEN_INVALID\""));
    }

    @Test
    @DisplayName("탈퇴 사용자 acToken으로 /api/users/me 호출 시 401 TOKEN_INVALID를 반환한다")
    void getMe_withDeletedUserAccessToken_returns401() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = "deleted-user-access-token";
        Claims claims = Jwts.claims().setSubject(userId);

        when(jwtProvider.parseToken(token)).thenReturn(claims);
        when(authTokenCache.getOrLoad(eq(userId), eq(token), any()))
                .thenAnswer(invocation -> {
                    Supplier<UserDetails> loader = invocation.getArgument(2);
                    return loader.get();
                });
        when(userDetailsService.loadUserByUsername(userId))
                .thenThrow(new TokenException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TOKEN_INVALID.code()))
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"TOKEN_INVALID\""));
    }

    @TestConfiguration
    static class TestCorsConfig {
        @Bean("corsConfigurationSource")
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedOriginPattern("*");
            config.addAllowedMethod("*");
            config.addAllowedHeader("*");

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return source;
        }
    }
}

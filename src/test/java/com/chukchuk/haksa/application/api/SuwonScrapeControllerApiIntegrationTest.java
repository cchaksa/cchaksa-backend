package com.chukchuk.haksa.application.api;

import com.chukchuk.haksa.application.portal.PortalSyncService;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.infrastructure.portal.repository.PortalRepository;
import com.chukchuk.haksa.support.ApiControllerWebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuwonScrapeController.class)
@AutoConfigureMockMvc(addFilters = false)
class SuwonScrapeControllerApiIntegrationTest extends ApiControllerWebMvcTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortalRepository portalRepository;

    @MockBean
    private PortalCredentialStore portalCredentialStore;

    @MockBean
    private AcademicCache academicCache;

    @MockBean
    private PortalSyncService portalSyncService;

    @MockBean
    private UserService userService;

    @MockBean
    private StudentService studentService;

    @Test
    @DisplayName("portal login 성공 시 성공 응답을 반환한다")
    void login_success() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());

        mockMvc.perform(post("/api/suwon-scrape/login")
                        .param("username", "portal-id")
                        .param("password", "portal-pw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(portalCredentialStore).save(eq(userId.toString()), eq("portal-id"), eq("portal-pw"));
    }

    @Test
    @DisplayName("이미 연동된 사용자가 start 호출 시 U03 예외를 반환한다")
    void start_alreadyConnected() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());
        when(userService.getUserById(userId)).thenReturn(connectedUser());

        mockMvc.perform(post("/api/suwon-scrape/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("U03"));
    }

    @Test
    @DisplayName("미연동 사용자가 refresh 호출 시 U04 예외를 반환한다")
    void refresh_notConnected() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId, UUID.randomUUID());
        when(userService.getUserById(userId)).thenReturn(disconnectedUser());

        mockMvc.perform(post("/api/suwon-scrape/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("U04"));
    }

    private static User connectedUser() {
        User user = User.builder()
                .email("connected@example.com")
                .profileNickname("connected")
                .build();
        user.markPortalConnected(Instant.now());
        return user;
    }

    private static User disconnectedUser() {
        return User.builder()
                .email("disconnected@example.com")
                .profileNickname("disconnected")
                .build();
    }
}

// 포털 클라이언트의 HTTP 상태별 예외 매핑을 검증한다.
package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PortalClientTest {

    @Test
    void scrapeAllMapsUnauthorizedToPortalLoginFailed() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PortalClient client = new PortalClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "https://crawler.example");

        server.expect(requestTo("https://crawler.example/scrape"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.scrapeAll("student", "password"))
                .isInstanceOfSatisfying(PortalScrapeException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PORTAL_LOGIN_FAILED));

        server.verify();
    }

    @Test
    void scrapeAllMapsLockedToPortalAccountLocked() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        PortalClient client = new PortalClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "https://crawler.example");

        server.expect(requestTo("https://crawler.example/scrape"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.LOCKED));

        assertThatThrownBy(() -> client.scrapeAll("student", "password"))
                .isInstanceOfSatisfying(PortalScrapeException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PORTAL_ACCOUNT_LOCKED));

        server.verify();
    }
}

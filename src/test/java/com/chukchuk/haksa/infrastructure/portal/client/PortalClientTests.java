// PortalClient의 포털 로그인 검증 HTTP 호출과 오류 매핑을 검증하는 테스트
package com.chukchuk.haksa.infrastructure.portal.client;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalLoginException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortalClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void validateLogin_postsCredentialToLoginEndpoint() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        PortalClient client = clientWithServer(204, requestBody);

        client.validateLogin("17019013", "pw");

        assertThat(requestBody.get()).contains("\"username\":\"17019013\"");
        assertThat(requestBody.get()).contains("\"password\":\"pw\"");
    }

    @Test
    void validateLogin_mapsUnauthorizedToPortalLoginFailed() throws Exception {
        PortalClient client = clientWithServer(401, new AtomicReference<>());

        assertThatThrownBy(() -> client.validateLogin("17019013", "wrong"))
                .isInstanceOf(PortalLoginException.class)
                .satisfies(ex -> assertThat(((PortalLoginException) ex).getCode()).isEqualTo(ErrorCode.PORTAL_LOGIN_FAILED.code()));
    }

    private PortalClient clientWithServer(int status, AtomicReference<String> requestBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login", exchange -> respond(exchange, status, requestBody));
        server.start();

        PortalClient client = new PortalClient();
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:" + server.getAddress().getPort());
        return client;
    }

    private void respond(HttpExchange exchange, int status, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}

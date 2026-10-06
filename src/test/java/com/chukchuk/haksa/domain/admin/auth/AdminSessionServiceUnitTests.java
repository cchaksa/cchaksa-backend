package com.chukchuk.haksa.domain.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import com.chukchuk.haksa.domain.admin.auth.model.AdminSession;
import com.chukchuk.haksa.domain.admin.auth.model.AdminStatus;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminSessionRepository;
import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import com.chukchuk.haksa.domain.admin.auth.service.AdminSessionService;
import com.chukchuk.haksa.domain.admin.auth.service.AdminTokenCodec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceUnitTests {
  private static final Instant NOW = Instant.parse("2026-09-29T00:00:00Z");

  @Mock private AdminSessionRepository repository;
  @Mock private AdminTokenCodec tokenCodec;

  private AdminAuthProperties properties;
  private AdminSessionService sessionService;

  @BeforeEach
  void setUp() {
    properties = new AdminAuthProperties();
    properties.setIdleTimeout(Duration.ofMinutes(30));
    properties.setAbsoluteTimeout(Duration.ofHours(8));
    sessionService =
        new AdminSessionService(
            repository, tokenCodec, properties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void activeSessionAuthenticatesAndRefreshesIdleExpiry() {
    AdminAccount account = account(AdminStatus.ACTIVE);
    AdminSession session =
        new AdminSession(
            UUID.randomUUID(),
            account,
            "stored-hash",
            NOW.plus(Duration.ofHours(8)),
            NOW.plus(Duration.ofMinutes(5)),
            NOW.minusSeconds(30));
    when(tokenCodec.hash("raw-session")).thenReturn("stored-hash");
    when(repository.findByTokenHash("stored-hash")).thenReturn(Optional.of(session));

    assertThat(sessionService.authenticate("raw-session"))
        .hasValueSatisfying(
            principal -> {
              assertThat(principal.adminAccountId()).isEqualTo(account.getId());
              assertThat(principal.role()).isEqualTo(AdminRole.CS_AGENT);
            });
    assertThat(session.getIdleExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
  }

  @Test
  void tamperedExpiredAndInactiveSessionsAreRejected() {
    when(tokenCodec.hash("tampered")).thenReturn("unknown-hash");
    when(repository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());
    assertThat(sessionService.authenticate("tampered")).isEmpty();

    AdminSession expired =
        new AdminSession(
            UUID.randomUUID(),
            account(AdminStatus.ACTIVE),
            "expired-hash",
            NOW.minusSeconds(1),
            NOW.plusSeconds(60),
            NOW.minusSeconds(60));
    when(tokenCodec.hash("expired")).thenReturn("expired-hash");
    when(repository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expired));
    assertThat(sessionService.authenticate("expired")).isEmpty();

    AdminSession inactive =
        new AdminSession(
            UUID.randomUUID(),
            account(AdminStatus.INACTIVE),
            "inactive-hash",
            NOW.plusSeconds(60),
            NOW.plusSeconds(60),
            NOW.minusSeconds(60));
    when(tokenCodec.hash("inactive")).thenReturn("inactive-hash");
    when(repository.findByTokenHash("inactive-hash")).thenReturn(Optional.of(inactive));
    assertThat(sessionService.authenticate("inactive")).isEmpty();
  }

  @Test
  void revokeAllDelegatesWithStableAuditTime() {
    UUID adminAccountId = UUID.randomUUID();
    when(repository.revokeAllByAdminAccountId(adminAccountId, NOW)).thenReturn(2);

    assertThat(sessionService.revokeAll(adminAccountId)).isEqualTo(2);
    verify(repository).revokeAllByAdminAccountId(adminAccountId, NOW);
  }

  private AdminAccount account(AdminStatus status) {
    return new AdminAccount(
        UUID.randomUUID(),
        "cs.agent",
        "password-hash",
        "CS 담당자",
        AdminRole.CS_AGENT,
        status,
        "bootstrap");
  }
}

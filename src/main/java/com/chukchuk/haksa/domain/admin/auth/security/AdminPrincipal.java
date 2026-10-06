package com.chukchuk.haksa.domain.admin.auth.security;

import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import java.security.Principal;
import java.util.UUID;

/** 인증된 관리자 계정과 현재 세션을 식별한다. */
public record AdminPrincipal(
    UUID adminAccountId, UUID sessionId, String displayName, AdminRole role) implements Principal {
  @Override
  public String getName() {
    return adminAccountId.toString();
  }
}

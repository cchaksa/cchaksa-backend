package com.chukchuk.haksa.support;

import com.chukchuk.haksa.domain.admin.auth.security.AdminSessionAuthenticationFilter;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilter;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Controller MVC 테스트에서 인증 사용자와 공통 MockBean을 제공한다. */
public abstract class ApiControllerWebMvcTestSupport {

  @MockBean protected JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean protected AdminSessionAuthenticationFilter adminSessionAuthenticationFilter;

  @MockBean protected JpaMetamodelMappingContext jpaMetamodelMappingContext;

  /**
   * 지정한 사용자로 인증 컨텍스트를 구성한다.
   *
   * @param userId 사용자 식별자
   * @param ignoredStudentId 이전 테스트 호환성을 위해 유지하는 학생 식별자
   */
  protected void authenticate(UUID userId, UUID ignoredStudentId) {
    authenticate(userId);
  }

  /**
   * 지정한 사용자로 인증 컨텍스트를 구성한다.
   *
   * @param userId 사용자 식별자
   */
  protected void authenticate(UUID userId) {
    CustomUserDetails principal =
        new CustomUserDetails(userId, "test@example.com", "tester", null, false);

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}

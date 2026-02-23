package com.chukchuk.haksa.support;

import com.chukchuk.haksa.global.security.CustomUserDetails;
import com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public abstract class ApiControllerWebMvcTestSupport {

    @MockBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    protected JpaMetamodelMappingContext jpaMetamodelMappingContext;

    protected void authenticate(UUID userId, UUID ignoredStudentId) {
        authenticate(userId);
    }

    protected void authenticate(UUID userId) {
        CustomUserDetails principal = new CustomUserDetails(
                userId,
                "test@example.com",
                "tester",
                null,
                false
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                AuthorityUtils.NO_AUTHORITIES
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}

package com.chukchuk.haksa.domain.testsupport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.testsupport.config.TestSupportProductionNotFoundConfig;
import com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
    controllers = TestSupportProductionHttpTest.ProbeController.class,
    properties = {
      "LOG_PATH=/tmp/cchaksa-test-support",
      "spring.mvc.throw-exception-if-no-handler-found=true",
      "spring.web.resources.add-mappings=false"
    })
@ActiveProfiles("prod")
@Import(TestSupportProductionNotFoundConfig.class)
class TestSupportProductionHttpTest {
  @Autowired private MockMvc mockMvc;
  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @BeforeEach
  void letMockedJwtFilterContinue() throws Exception {
    doAnswer(
            invocation -> {
              FilterChain chain = invocation.getArgument(2);
              chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(jwtAuthenticationFilter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
  }

  @Test
  void productionReturnsNotFoundForTestSupportPath() throws Exception {
    mockMvc.perform(get("/api/test/users")).andExpect(status().isNotFound());
  }

  @RestController
  static class ProbeController {
    @GetMapping("/probe")
    String probe() {
      return "ok";
    }
  }
}

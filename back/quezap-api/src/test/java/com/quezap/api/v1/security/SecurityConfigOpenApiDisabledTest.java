package com.quezap.api.v1.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.junit.jupiter.api.Test;

@SpringBootTest(
    properties = {
      "spring.main.banner-mode=off",
      "springdoc.api-docs.enabled=false",
      "springdoc.swagger-ui.enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigOpenApiDisabledTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldRejectUnauthenticatedAccessToOpenApiDocsWhenDisabled() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void shouldForbidAuthenticatedAccessToOpenApiDocsAndSwaggerWhenDisabled() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    mockMvc
        .perform(MockMvcRequestBuilders.get("/swagger-ui/index.html"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }
}

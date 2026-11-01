package com.quezap.api.v1.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.junit.jupiter.api.Test;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldAllowPublicAccessToQuezapsListing() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/apiv1/quezaps"))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void shouldRejectUnauthenticatedQuezapCreation() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/apiv1/quezaps")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Quizz\"}"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldRejectUnauthenticatedSessionListing() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/apiv1/sessions"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldRejectUnauthenticatedSessionCreation() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/apiv1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Session\"}"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldAllowPublicJoinEndpointWithoutAuthentication() throws Exception {
    // When calling join without auth with bad body, it returns 400 (not 401 Unauthorized)
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/apiv1/sessions/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }
}

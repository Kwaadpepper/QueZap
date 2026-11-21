package com.quezap.api.v1.security;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Test;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private RefreshTokenRevocationStore revocationStore;

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

  @Test
  void shouldRejectUnauthenticatedQuestionsListing() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/apiv1/questions"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldRejectUnauthenticatedActuatorEnv() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/actuator/env"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldAllowPublicAccessToActuatorHealth() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/actuator/health"))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void shouldRejectUntrustedCorsOrigin() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.options("/apiv1/quezaps")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void shouldAllowConfiguredCorsOrigin() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.options("/apiv1/quezaps")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Origin", "http://localhost:4200"));
  }

  @Test
  @WithMockUser(username = "01900000-0000-7000-8000-000000000001")
  void shouldReturn404ForNonExistentThemeDelete() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/apiv1/themes/" + UuidV7.randomUuid()))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  @WithMockUser(username = "01900000-0000-7000-8000-000000000001")
  void shouldReturn404ForNonExistentQuestionDelete() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete("/apiv1/questions/" + UuidV7.randomUuid()))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  void shouldReturn400ForPartialPagination() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/apiv1/themes?page=1"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  void shouldAllowParticipantTokenHeadersInCorsPreflight() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.options("/apiv1/sessions/join")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-Participant-Token"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
            MockMvcResultMatchers.header()
                .string(
                    "Access-Control-Allow-Headers",
                    org.hamcrest.Matchers.containsString("X-Participant-Token")));
  }

  @Test
  void shouldRejectNonAccessJwtTokensAsBearerAuth() throws Exception {
    final var exp = Instant.now().getEpochSecond() + 3600;
    final var sub = UUID.randomUUID().toString();
    final var refreshJwt =
        jwtService.generateToken(
            JwtToken.JwtPayload.builder(sub, exp).claim("type", "refresh").build());
    final var resetJwt =
        jwtService.generateToken(
            JwtToken.JwtPayload.builder(sub, exp).claim("type", "password-reset").build());
    final var untypedJwt = jwtService.generateToken(JwtToken.JwtPayload.builder(sub, exp).build());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/apiv1/questions")
                .header("Authorization", "Bearer " + refreshJwt))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/apiv1/questions")
                .header("Authorization", "Bearer " + resetJwt))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/apiv1/questions")
                .header("Authorization", "Bearer " + untypedJwt))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void shouldVerifyResetTokenViaPostAndRejectGet() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/apiv1/auth/verify-reset-token?token=abc"))
        .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/apiv1/auth/verify-reset-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid-token\"}"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void shouldPersistRevokedRefreshTokenInDatabase() {
    final var token = "db-persisted-refresh-token-" + UUID.randomUUID();
    revocationStore.revoke(token, Instant.now().getEpochSecond() + 3600);
    org.assertj.core.api.Assertions.assertThat(revocationStore.isRevoked(token)).isTrue();
  }

  @Test
  void shouldRejectRefreshWithoutCookieAndClearCookieOnLogout() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.post("/apiv1/auth/refresh"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());

    mockMvc
        .perform(MockMvcRequestBuilders.post("/apiv1/auth/logout"))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andExpect(
            MockMvcResultMatchers.header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("quezap_refresh_token="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))));
  }
}

package com.quezap.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtAuthenticationFilterTest {

  private JwtService jwtService;
  private JwtAuthenticationFilter filter;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    jwtService = mock(JwtService.class);
    filter = new JwtAuthenticationFilter(jwtService);
    filterChain = mock(FilterChain.class);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAuthenticateValidAccessToken() throws Exception {
    final var userId = UUID.randomUUID().toString();
    final var payload =
        JwtToken.JwtPayload.builder(userId, Instant.now().getEpochSecond() + 3600)
            .claim("type", "access")
            .build();
    when(jwtService.validateAndParse("valid-access-token")).thenReturn(payload);

    final var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-access-token");
    final var response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    final var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getName()).isEqualTo(userId);
    verify(filterChain).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {"refresh", "activation", "password-reset", "participation", "unknown"})
  void shouldRejectNonAccessTokenTypes(String tokenType) throws Exception {
    final var userId = UUID.randomUUID().toString();
    final var payload =
        JwtToken.JwtPayload.builder(userId, Instant.now().getEpochSecond() + 3600)
            .claim("type", tokenType)
            .build();
    when(jwtService.validateAndParse("non-access-token")).thenReturn(payload);

    final var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer non-access-token");
    final var response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldRejectUntypedToken() throws Exception {
    final var userId = UUID.randomUUID().toString();
    final var payload =
        JwtToken.JwtPayload.builder(userId, Instant.now().getEpochSecond() + 3600).build();
    when(jwtService.validateAndParse("untyped-token")).thenReturn(payload);

    final var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer untyped-token");
    final var response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }
}

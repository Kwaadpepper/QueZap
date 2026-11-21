package com.quezap.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.mock.web.MockHttpServletRequest;

import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

  @Test
  void shouldReturnNullWhenRequestIsNull() {
    assertThat(ClientIpResolver.resolve(null)).isNull();
  }

  @Test
  void shouldPreferRealIpHeaderOverForwardedForAndRemoteAddr() {
    final var request = new MockHttpServletRequest();
    request.addHeader("X-Real-IP", "203.0.113.10");
    request.addHeader("X-Forwarded-For", "198.51.100.1, 198.51.100.2");
    request.setRemoteAddr("127.0.0.1");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
  }

  @Test
  void shouldUseLastEntryInForwardedForWhenRealIpHeaderAbsent() {
    final var request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "10.0.0.1, 198.51.100.77, ");
    request.setRemoteAddr("127.0.0.1");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.77");
  }

  @Test
  void shouldFallBackToRemoteAddrWhenHeadersMissing() {
    final var request = new MockHttpServletRequest();
    request.setRemoteAddr("192.0.2.42");

    assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.0.2.42");
  }
}

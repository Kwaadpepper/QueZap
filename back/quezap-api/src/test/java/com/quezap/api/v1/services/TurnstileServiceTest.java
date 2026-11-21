package com.quezap.api.v1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import org.junit.jupiter.api.Test;

class TurnstileServiceTest {

  private static final String VERIFY_URL =
      "https://challenges.cloudflare.com/turnstile/v0/siteverify";

  @Test
  void shouldBeNoOpWhenSecretKeyIsBlank() {
    final var service = new TurnstileService("", VERIFY_URL, RestClient.builder().build());

    assertThat(service.isEnabled()).isFalse();
    assertThatCode(() -> service.verifyOrThrow(null, null)).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectMissingTokenWhenEnabled() {
    final var service =
        new TurnstileService("secret-key", VERIFY_URL, RestClient.builder().build());

    assertThat(service.isEnabled()).isTrue();
    assertThatThrownBy(() -> service.verifyOrThrow(" ", "127.0.0.1"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldAcceptValidTurnstileResponse() {
    final var builder = RestClient.builder();
    final var server = MockRestServiceServer.bindTo(builder).build();
    final var service = new TurnstileService("secret-key", VERIFY_URL, builder.build());

    server
        .expect(requestTo(VERIFY_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

    assertThatCode(() -> service.verifyOrThrow("valid-token", "127.0.0.1"))
        .doesNotThrowAnyException();
    server.verify();
  }

  @Test
  void shouldRejectFailedTurnstileResponse() {
    final var builder = RestClient.builder();
    final var server = MockRestServiceServer.bindTo(builder).build();
    final var service = new TurnstileService("secret-key", VERIFY_URL, builder.build());

    server
        .expect(requestTo(VERIFY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> service.verifyOrThrow("bad-token", null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    server.verify();
  }
}

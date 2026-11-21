package com.quezap.api.v1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionSseServiceTest {

  private SessionSseService sseService;

  @BeforeEach
  void setUp() {
    sseService =
        new SessionSseService(Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void shouldEnforcePerSessionEmitterCapAndCleanupOnClearSession() {
    final var sessionId = UuidV7.randomUuid();

    for (var i = 0; i < SessionSseService.MAX_EMITTERS_PER_SESSION; i++) {
      sseService.registerEmitter(sessionId, "10.0." + (i / 25) + "." + (i % 25));
    }

    assertThat(sseService.getActiveEmittersCount(sessionId))
        .isEqualTo(SessionSseService.MAX_EMITTERS_PER_SESSION);

    assertThatThrownBy(() -> sseService.registerEmitter(sessionId, "10.99.99.99"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

    sseService.clearSession(sessionId);
    assertThat(sseService.getActiveEmittersCount(sessionId)).isZero();
    assertThat(sseService.getActiveEmittersCountForIp("10.0.0.0")).isZero();

    // New emitter succeeds after cleanup
    final var emitter = sseService.registerEmitter(sessionId, "10.0.0.0");
    assertThat(emitter).isNotNull();
    assertThat(sseService.getActiveEmittersCountForIp("10.0.0.0")).isEqualTo(1);
  }

  @Test
  void shouldEnforcePerIpEmitterCapAcrossSessionsAndDecrementOnClear() {
    final var clientIp = "192.168.1.50";
    final UUID firstSession = UuidV7.randomUuid();
    final UUID secondSession = UuidV7.randomUuid();

    for (var i = 0; i < SessionSseService.MAX_EMITTERS_PER_IP; i++) {
      final var targetSession = i < 15 ? firstSession : secondSession;
      sseService.registerEmitter(targetSession, clientIp);
    }

    assertThat(sseService.getActiveEmittersCountForIp(clientIp))
        .isEqualTo(SessionSseService.MAX_EMITTERS_PER_IP);

    assertThatThrownBy(() -> sseService.registerEmitter(secondSession, clientIp))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

    // Another IP is unaffected
    sseService.registerEmitter(secondSession, "192.168.1.51");
    assertThat(sseService.getActiveEmittersCountForIp("192.168.1.51")).isEqualTo(1);

    // Clearing firstSession decrements exactly the 15 emitters from firstSession
    sseService.clearSession(firstSession);
    assertThat(sseService.getActiveEmittersCountForIp(clientIp))
        .isEqualTo(SessionSseService.MAX_EMITTERS_PER_IP - 15);
  }

  @Test
  void shouldIssueSingleUseSessionBoundSseTicketsWithTtl() {
    final var mutableTime = new Instant[] {Instant.parse("2026-11-18T10:00:00Z")};
    final var mutableClock =
        new Clock() {
          @Override
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return mutableTime[0];
          }
        };

    final var ticketService = new SessionSseService(mutableClock);
    final var sessionA = UuidV7.randomUuid();
    final var sessionB = UuidV7.randomUuid();

    final var ticket1 = ticketService.issueTicket(sessionA);
    assertThat(ticket1).startsWith("sset_");

    // Wrong session consumes/invalidates or fails
    assertThat(ticketService.consumeTicket(sessionB, ticket1)).isFalse();
    // Single-use: already removed
    assertThat(ticketService.consumeTicket(sessionA, ticket1)).isFalse();

    // Valid ticket works once
    final var ticket2 = ticketService.issueTicket(sessionA);
    assertThat(ticketService.consumeTicket(sessionA, ticket2)).isTrue();
    assertThat(ticketService.consumeTicket(sessionA, ticket2)).isFalse();

    // Expired ticket fails
    final var ticket3 = ticketService.issueTicket(sessionA);
    mutableTime[0] = mutableTime[0].plusSeconds(61);
    assertThat(ticketService.consumeTicket(sessionA, ticket3)).isFalse();
  }
}

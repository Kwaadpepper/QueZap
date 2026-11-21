package com.quezap.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.mock.web.MockHttpServletRequest;

import org.junit.jupiter.api.Test;

class SessionEndpointRateLimiterTest {

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant initial) {
      this.now = initial;
    }

    void advanceSeconds(long seconds) {
      this.now = this.now.plusSeconds(seconds);
    }

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
      return now;
    }
  }

  @Test
  void shouldEnforceLimitWithinWindowAndResetAfterExpiry() {
    final var clock = new MutableClock(Instant.parse("2026-11-18T10:00:00Z"));
    final var limiter = new SessionEndpointRateLimiter(3, 60L, clock);

    assertThat(limiter.tryAcquire("join:1.2.3.4")).isTrue();
    assertThat(limiter.tryAcquire("join:1.2.3.4")).isTrue();
    assertThat(limiter.tryAcquire("join:1.2.3.4")).isTrue();
    assertThat(limiter.tryAcquire("join:1.2.3.4")).isFalse();

    // Different key is independent
    assertThat(limiter.tryAcquire("join:5.6.7.8")).isTrue();

    // After 60 seconds, window resets
    clock.advanceSeconds(60);
    assertThat(limiter.tryAcquire("join:1.2.3.4")).isTrue();
  }

  @Test
  void shouldBoundMapEntriesAndPurgeExpiredOnOverflow() {
    final var clock = new MutableClock(Instant.parse("2026-11-18T10:00:00Z"));
    final var limiter = new SessionEndpointRateLimiter(5, 60L, clock);

    for (var i = 0; i < SessionEndpointRateLimiter.MAX_ENTRIES; i++) {
      assertThat(limiter.tryAcquire("k:" + i)).isTrue();
    }
    assertThat(limiter.size()).isEqualTo(SessionEndpointRateLimiter.MAX_ENTRIES);

    // New key fails closed when all 10,000 entries are active
    assertThat(limiter.tryAcquire("k:overflow")).isFalse();

    // Advancing past window allows purging expired entries
    clock.advanceSeconds(61);
    assertThat(limiter.tryAcquire("k:after-expiry")).isTrue();
    assertThat(limiter.size()).isEqualTo(1);
  }

  @Test
  void shouldResolveClientIpSafely() {
    assertThat(SessionEndpointRateLimiter.resolveClientIp(null)).isEqualTo("unknown");

    final var reqRealIp = new MockHttpServletRequest();
    reqRealIp.addHeader("X-Real-IP", "203.0.113.10");
    reqRealIp.addHeader("X-Forwarded-For", "198.51.100.1, 198.51.100.2");
    reqRealIp.setRemoteAddr("127.0.0.1");
    assertThat(SessionEndpointRateLimiter.resolveClientIp(reqRealIp)).isEqualTo("203.0.113.10");

    final var reqForwarded = new MockHttpServletRequest();
    reqForwarded.addHeader("X-Forwarded-For", "198.51.100.1, 198.51.100.2");
    reqForwarded.setRemoteAddr("127.0.0.1");
    assertThat(SessionEndpointRateLimiter.resolveClientIp(reqForwarded)).isEqualTo("198.51.100.2");

    final var reqRemote = new MockHttpServletRequest();
    reqRemote.setRemoteAddr("10.0.0.5");
    assertThat(SessionEndpointRateLimiter.resolveClientIp(reqRemote)).isEqualTo("10.0.0.5");
  }
}

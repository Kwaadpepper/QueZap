package com.quezap.api.v1.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

  @Test
  void shouldIsolateFailuresByClientIpAndIdentifier() {
    final var limiter = new LoginRateLimiter(2, 10, 60L, 100);

    limiter.recordLoginFailure("198.51.100.1", "alice");
    limiter.recordLoginFailure("198.51.100.1", "alice");

    assertThat(limiter.isLoginRateLimited("198.51.100.1", "alice")).isTrue();
    assertThat(limiter.isLoginRateLimited("198.51.100.2", "alice")).isFalse();
    assertThat(limiter.isLoginRateLimited("198.51.100.1", "bob")).isFalse();
  }

  @Test
  void shouldEnforceGlobalIpLimitAcrossMultipleIdentifiers() {
    final var limiter = new LoginRateLimiter(5, 3, 60L, 100);

    limiter.recordLoginFailure("198.51.100.1", "user1");
    limiter.recordLoginFailure("198.51.100.1", "user2");
    limiter.recordLoginFailure("198.51.100.1", "user3");

    assertThat(limiter.isLoginRateLimited("198.51.100.1", "user4")).isTrue();
    assertThat(limiter.isLoginRateLimited("198.51.100.2", "user4")).isFalse();
  }

  @Test
  void shouldKeepGlobalIpCounterWhenResettingSingleIdentifier() {
    final var limiter = new LoginRateLimiter(2, 3, 60L, 100);

    limiter.recordLoginFailure("198.51.100.1", "user1");
    limiter.recordLoginFailure("198.51.100.1", "user2");
    limiter.resetLogin("198.51.100.1", "user1");
    limiter.recordLoginFailure("198.51.100.1", "user3");

    assertThat(limiter.isLoginRateLimited("198.51.100.1", "user4")).isTrue();
  }

  @Test
  void shouldEnforceScopedTryAcquireLimitPerIp() {
    final var limiter = new LoginRateLimiter();

    for (int i = 0; i < 5; i++) {
      assertThat(limiter.tryAcquire("forgot", "203.0.113.5", 5, 60L)).isTrue();
    }
    assertThat(limiter.tryAcquire("forgot", "203.0.113.5", 5, 60L)).isFalse();
    assertThat(limiter.tryAcquire("forgot", "203.0.113.6", 5, 60L)).isTrue();
  }

  @Test
  void shouldEnforceMaxEntriesCapacity() {
    final var limiter = new LoginRateLimiter(5, 20, 60L, 10);

    for (int i = 0; i < 25; i++) {
      limiter.tryAcquire("register", "192.0.2." + i, 10, 60L);
    }

    assertThat(limiter.trackedEntryCount()).isLessThanOrEqualTo(10);
  }
}

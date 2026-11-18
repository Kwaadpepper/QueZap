package com.quezap.api.v1.security;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
  private static final int DEFAULT_MAX_ATTEMPTS = 5;
  private static final long DEFAULT_WINDOW_SECONDS = 60L;
  private static final int CLEANUP_THRESHOLD = 512;

  private final int maxAttempts;
  private final long windowSeconds;
  private final ConcurrentHashMap<String, AttemptWindow> attemptsByKey = new ConcurrentHashMap<>();

  public LoginRateLimiter() {
    this(DEFAULT_MAX_ATTEMPTS, DEFAULT_WINDOW_SECONDS);
  }

  public LoginRateLimiter(int maxAttempts, long windowSeconds) {
    this.maxAttempts = maxAttempts;
    this.windowSeconds = windowSeconds;
  }

  public boolean isRateLimited(String identifier) {
    final var key = normalizeKey(identifier);
    final var window = attemptsByKey.get(key);
    if (window == null) {
      return false;
    }
    final var now = Instant.now().getEpochSecond();
    if (window.expiresAtEpochSeconds() <= now) {
      attemptsByKey.remove(key, window);
      return false;
    }
    return window.count() >= maxAttempts;
  }

  public void recordFailure(String identifier) {
    final var key = normalizeKey(identifier);
    final var now = Instant.now().getEpochSecond();
    if (attemptsByKey.size() >= CLEANUP_THRESHOLD) {
      purgeExpired(now);
    }
    attemptsByKey.compute(
        key,
        (ignoredKey, current) -> {
          if (current == null || current.expiresAtEpochSeconds() <= now) {
            return new AttemptWindow(1, now + windowSeconds);
          }
          return new AttemptWindow(current.count() + 1, current.expiresAtEpochSeconds());
        });
  }

  public void reset(String identifier) {
    attemptsByKey.remove(normalizeKey(identifier));
  }

  public void clearAll() {
    attemptsByKey.clear();
  }

  private String normalizeKey(String identifier) {
    return identifier.trim().toLowerCase(Locale.ROOT);
  }

  private void purgeExpired(long nowEpochSeconds) {
    attemptsByKey.entrySet().removeIf(e -> e.getValue().expiresAtEpochSeconds() <= nowEpochSeconds);
  }

  private record AttemptWindow(int count, long expiresAtEpochSeconds) {}
}

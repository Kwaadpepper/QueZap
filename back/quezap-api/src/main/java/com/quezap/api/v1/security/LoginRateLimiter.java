package com.quezap.api.v1.security;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.jspecify.annotations.Nullable;

@Component
public class LoginRateLimiter {
  private static final int DEFAULT_MAX_ATTEMPTS = 5;
  private static final int DEFAULT_MAX_IP_ATTEMPTS = 20;
  private static final long DEFAULT_WINDOW_SECONDS = 60L;
  private static final int DEFAULT_MAX_ENTRIES = 10_000;
  private static final int CLEANUP_THRESHOLD = 512;

  private final int maxAttempts;
  private final int maxIpAttempts;
  private final long windowSeconds;
  private final int maxEntries;
  private final ConcurrentHashMap<String, AttemptWindow> attemptsByKey = new ConcurrentHashMap<>();
  private final Object capacityLock = new Object();

  @Autowired
  public LoginRateLimiter() {
    this(
        DEFAULT_MAX_ATTEMPTS, DEFAULT_MAX_IP_ATTEMPTS, DEFAULT_WINDOW_SECONDS, DEFAULT_MAX_ENTRIES);
  }

  public LoginRateLimiter(int maxAttempts, long windowSeconds) {
    this(
        maxAttempts,
        Math.max(maxAttempts * 4, DEFAULT_MAX_IP_ATTEMPTS),
        windowSeconds,
        DEFAULT_MAX_ENTRIES);
  }

  public LoginRateLimiter(int maxAttempts, int maxIpAttempts, long windowSeconds, int maxEntries) {
    if (maxAttempts <= 0 || maxIpAttempts <= 0 || windowSeconds <= 0 || maxEntries <= 0) {
      throw new IllegalArgumentException("Rate limiter thresholds and capacity must be positive");
    }
    this.maxAttempts = maxAttempts;
    this.maxIpAttempts = maxIpAttempts;
    this.windowSeconds = windowSeconds;
    this.maxEntries = maxEntries;
  }

  public boolean isRateLimited(String identifier) {
    return isRateLimitedForKey(compositeLoginKey(null, identifier), maxAttempts);
  }

  public void recordFailure(String identifier) {
    recordAttemptForKey(compositeLoginKey(null, identifier), windowSeconds);
  }

  public void reset(String identifier) {
    attemptsByKey.remove(compositeLoginKey(null, identifier));
  }

  public boolean isLoginRateLimited(@Nullable String clientIp, String identifier) {
    return isRateLimitedForKey(compositeLoginKey(clientIp, identifier), maxAttempts)
        || isRateLimitedForKey(ipLoginKey(clientIp), maxIpAttempts);
  }

  public void recordLoginFailure(@Nullable String clientIp, String identifier) {
    recordAttemptForKey(compositeLoginKey(clientIp, identifier), windowSeconds);
    recordAttemptForKey(ipLoginKey(clientIp), windowSeconds);
  }

  public void resetLogin(@Nullable String clientIp, String identifier) {
    attemptsByKey.remove(compositeLoginKey(clientIp, identifier));
  }

  public boolean tryAcquire(
      String scope, @Nullable String clientIp, int limit, long customWindowSeconds) {
    final var key = normalizeKey(scope) + ":ip:" + normalizeIp(clientIp);
    final var now = Instant.now().getEpochSecond();
    final var allowed = new boolean[1];
    synchronized (capacityLock) {
      ensureCapacity(key, now);
      attemptsByKey.compute(
          key,
          (ignoredKey, current) -> {
            if (current == null || current.expiresAtEpochSeconds() <= now) {
              allowed[0] = true;
              return new AttemptWindow(1, now + customWindowSeconds);
            }
            if (current.count() >= limit) {
              allowed[0] = false;
              return current;
            }
            allowed[0] = true;
            return new AttemptWindow(current.count() + 1, current.expiresAtEpochSeconds());
          });
    }
    return allowed[0];
  }

  public void clearAll() {
    attemptsByKey.clear();
  }

  public int trackedEntryCount() {
    return attemptsByKey.size();
  }

  private boolean isRateLimitedForKey(String key, int limit) {
    final var window = attemptsByKey.get(key);
    if (window == null) {
      return false;
    }
    final var now = Instant.now().getEpochSecond();
    if (window.expiresAtEpochSeconds() <= now) {
      attemptsByKey.remove(key, window);
      return false;
    }
    return window.count() >= limit;
  }

  private void recordAttemptForKey(String key, long ttlSeconds) {
    final var now = Instant.now().getEpochSecond();
    synchronized (capacityLock) {
      ensureCapacity(key, now);
      attemptsByKey.compute(
          key,
          (ignoredKey, current) -> {
            if (current == null || current.expiresAtEpochSeconds() <= now) {
              return new AttemptWindow(1, now + ttlSeconds);
            }
            return new AttemptWindow(current.count() + 1, current.expiresAtEpochSeconds());
          });
    }
  }

  private void ensureCapacity(String incomingKey, long nowEpochSeconds) {
    if (attemptsByKey.containsKey(incomingKey)) {
      return;
    }
    if (attemptsByKey.size() >= CLEANUP_THRESHOLD || attemptsByKey.size() >= maxEntries) {
      purgeExpired(nowEpochSeconds);
    }
    if (attemptsByKey.size() >= maxEntries) {
      final int toEvict = (attemptsByKey.size() - maxEntries) + 1;
      attemptsByKey.entrySet().stream()
          .sorted(
              Map.Entry.comparingByValue(
                  Comparator.comparingLong(AttemptWindow::expiresAtEpochSeconds)))
          .limit(toEvict)
          .map(Map.Entry::getKey)
          .toList()
          .forEach(attemptsByKey::remove);
    }
  }

  private static String compositeLoginKey(@Nullable String clientIp, String identifier) {
    return "login:id:" + normalizeIp(clientIp) + "|" + normalizeKey(identifier);
  }

  private static String ipLoginKey(@Nullable String clientIp) {
    return "login:ip:" + normalizeIp(clientIp);
  }

  private static String normalizeIp(@Nullable String clientIp) {
    if (clientIp == null || clientIp.isBlank()) {
      return "unknown";
    }
    return clientIp.trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeKey(String identifier) {
    return identifier.trim().toLowerCase(Locale.ROOT);
  }

  private void purgeExpired(long nowEpochSeconds) {
    attemptsByKey.entrySet().removeIf(e -> e.getValue().expiresAtEpochSeconds() <= nowEpochSeconds);
  }

  private record AttemptWindow(int count, long expiresAtEpochSeconds) {}
}

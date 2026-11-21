package com.quezap.api.v1.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

@Component
public class SessionEndpointRateLimiter {

  public static final int DEFAULT_MAX_REQUESTS = 30;
  public static final long DEFAULT_WINDOW_SECONDS = 60L;
  public static final int MAX_ENTRIES = 10_000;

  private final int maxRequests;
  private final long windowSeconds;
  private final Clock clock;
  private final ConcurrentHashMap<String, RateWindow> windows = new ConcurrentHashMap<>();

  @Autowired
  public SessionEndpointRateLimiter() {
    this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS, Clock.systemUTC());
  }

  public SessionEndpointRateLimiter(int maxRequests, long windowSeconds) {
    this(maxRequests, windowSeconds, Clock.systemUTC());
  }

  public SessionEndpointRateLimiter(int maxRequests, long windowSeconds, Clock clock) {
    this.maxRequests = maxRequests;
    this.windowSeconds = windowSeconds;
    this.clock = clock;
  }

  public boolean tryAcquire(String key) {
    final var now = clock.instant().getEpochSecond();
    if (windows.size() >= MAX_ENTRIES) {
      purgeExpired(now);
      if (!windows.containsKey(key) && windows.size() >= MAX_ENTRIES) {
        return false;
      }
    }

    final var updated =
        windows.compute(
            key,
            (ignored, current) -> {
              if (current == null || current.expiresAtEpochSeconds() <= now) {
                return new RateWindow(1, now + windowSeconds);
              }
              return new RateWindow(current.count() + 1, current.expiresAtEpochSeconds());
            });
    return updated != null && updated.count() <= maxRequests;
  }

  public void clearAll() {
    windows.clear();
  }

  public int size() {
    return windows.size();
  }

  public static String resolveClientIp(@Nullable HttpServletRequest request) {
    if (request == null) {
      return "unknown";
    }
    final var realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    final var forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      final var parts = forwardedFor.split(",", -1);
      for (var i = parts.length - 1; i >= 0; i--) {
        final var candidate = parts[i].trim();
        if (!candidate.isEmpty()) {
          return candidate;
        }
      }
    }
    final var remoteAddr = request.getRemoteAddr();
    return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr.trim() : "unknown";
  }

  private void purgeExpired(long nowEpochSeconds) {
    windows
        .entrySet()
        .removeIf(entry -> entry.getValue().expiresAtEpochSeconds() <= nowEpochSeconds);
  }

  private record RateWindow(int count, long expiresAtEpochSeconds) {}
}

package com.quezap.api.v1.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRevocationStore {
  private static final int CLEANUP_THRESHOLD = 256;

  private final ConcurrentHashMap<String, Long> revokedTokens = new ConcurrentHashMap<>();

  public void revoke(String token, long expiresAtEpochSeconds) {
    final var now = Instant.now().getEpochSecond();
    if (expiresAtEpochSeconds <= now) {
      return;
    }
    if (revokedTokens.size() >= CLEANUP_THRESHOLD) {
      purgeExpired(now);
    }
    revokedTokens.put(token, expiresAtEpochSeconds);
  }

  public boolean isRevoked(String token) {
    final var expiresAt = revokedTokens.get(token);
    if (expiresAt == null) {
      return false;
    }
    final var now = Instant.now().getEpochSecond();
    if (expiresAt <= now) {
      revokedTokens.remove(token, expiresAt);
      return false;
    }
    return true;
  }

  public void clear() {
    revokedTokens.clear();
  }

  private void purgeExpired(long nowEpochSeconds) {
    revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= nowEpochSeconds);
  }
}

package com.quezap.api.v1.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.RevokedRefreshTokenJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.RevokedRefreshTokenJpaRepository;

import org.jspecify.annotations.Nullable;

@Component
public class RefreshTokenRevocationStore {
  private static final int CLEANUP_THRESHOLD = 256;
  private static final int MAX_CACHE_ENTRIES = 10_000;

  private final ConcurrentHashMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
  private final @Nullable RevokedRefreshTokenJpaRepository revokedRefreshTokenRepository;
  private final Object capacityLock = new Object();

  public RefreshTokenRevocationStore() {
    this(null);
  }

  @Autowired
  public RefreshTokenRevocationStore(
      @Nullable RevokedRefreshTokenJpaRepository revokedRefreshTokenRepository) {
    this.revokedRefreshTokenRepository = revokedRefreshTokenRepository;
  }

  public void revoke(String token, long expiresAtEpochSeconds) {
    final var now = Instant.now().getEpochSecond();
    if (expiresAtEpochSeconds <= now) {
      return;
    }

    final var tokenHash = hashToken(token);
    cacheRevocation(tokenHash, expiresAtEpochSeconds, now);

    if (revokedRefreshTokenRepository != null
        && !revokedRefreshTokenRepository.existsById(tokenHash)) {
      try {
        revokedRefreshTokenRepository.save(
            new RevokedRefreshTokenJpaEntity(
                tokenHash, Instant.ofEpochSecond(expiresAtEpochSeconds)));
      } catch (DataIntegrityViolationException ignored) {
        // Token hash was concurrently persisted by another request
      }
    }
  }

  public boolean isRevoked(String token) {
    final var tokenHash = hashToken(token);
    final var now = Instant.now().getEpochSecond();
    final var cachedExpiresAt = revokedTokens.get(tokenHash);
    if (cachedExpiresAt != null) {
      if (cachedExpiresAt <= now) {
        revokedTokens.remove(tokenHash, cachedExpiresAt);
        return false;
      }
      return true;
    }

    if (revokedRefreshTokenRepository != null) {
      final var persisted = revokedRefreshTokenRepository.findById(tokenHash);
      if (persisted.isPresent()) {
        final var expiresAtEpoch = persisted.get().getExpiresAt().getEpochSecond();
        if (expiresAtEpoch <= now) {
          return false;
        }
        cacheRevocation(tokenHash, expiresAtEpoch, now);
        return true;
      }
    }

    return false;
  }

  @Scheduled(fixedRate = 3600000)
  public void cleanupExpired() {
    final var now = Instant.now();
    purgeExpired(now.getEpochSecond());
    if (revokedRefreshTokenRepository != null) {
      revokedRefreshTokenRepository.deleteByExpiresAtBefore(now);
    }
  }

  public void clear() {
    revokedTokens.clear();
    if (revokedRefreshTokenRepository != null) {
      revokedRefreshTokenRepository.deleteAllInBatch();
    }
  }

  static String hashToken(String token) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private void cacheRevocation(String tokenHash, long expiresAtEpochSeconds, long nowEpochSeconds) {
    synchronized (capacityLock) {
      if (!revokedTokens.containsKey(tokenHash)) {
        if (revokedTokens.size() >= CLEANUP_THRESHOLD) {
          purgeExpired(nowEpochSeconds);
        }
        if (revokedTokens.size() >= MAX_CACHE_ENTRIES) {
          final int toEvict = (revokedTokens.size() - MAX_CACHE_ENTRIES) + 1;
          revokedTokens.entrySet().stream()
              .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
              .limit(toEvict)
              .map(Map.Entry::getKey)
              .toList()
              .forEach(revokedTokens::remove);
        }
      }
      revokedTokens.put(tokenHash, expiresAtEpochSeconds);
    }
  }

  private void purgeExpired(long nowEpochSeconds) {
    revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= nowEpochSeconds);
  }
}

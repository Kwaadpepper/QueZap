package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "revoked_refresh_tokens",
    indexes = {@Index(name = "idx_revoked_refresh_tokens_expires_at", columnList = "expires_at")})
public class RevokedRefreshTokenJpaEntity {

  @Id
  @Column(name = "token_hash", nullable = false, updatable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @SuppressWarnings("NullAway.Init")
  protected RevokedRefreshTokenJpaEntity() {
    // For JPA
  }

  public RevokedRefreshTokenJpaEntity(String tokenHash, Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RevokedRefreshTokenJpaEntity that)) {
      return false;
    }
    return Objects.equals(tokenHash, that.tokenHash);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tokenHash);
  }
}

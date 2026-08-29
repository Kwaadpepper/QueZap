package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "credentials",
    indexes = {
      @Index(
          name = "idx_credentials_hashed_identifier",
          columnList = "hashed_identifier",
          unique = true)
    })
public class CredentialJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "hashed_identifier", nullable = false, length = 255, unique = true)
  private String hashedIdentifier;

  @Column(name = "hashed_password", nullable = false, length = 255)
  private String hashedPassword;

  @Column(name = "last_connection_at")
  private @Nullable Instant lastConnectionAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @SuppressWarnings("NullAway.Init")
  protected CredentialJpaEntity() {
    // For JPA
  }

  public CredentialJpaEntity(
      UUID id,
      String hashedIdentifier,
      String hashedPassword,
      @Nullable Instant lastConnectionAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.hashedIdentifier = hashedIdentifier;
    this.hashedPassword = hashedPassword;
    this.lastConnectionAt = lastConnectionAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getHashedIdentifier() {
    return hashedIdentifier;
  }

  public void setHashedIdentifier(String hashedIdentifier) {
    this.hashedIdentifier = hashedIdentifier;
  }

  public String getHashedPassword() {
    return hashedPassword;
  }

  public void setHashedPassword(String hashedPassword) {
    this.hashedPassword = hashedPassword;
  }

  public @Nullable Instant getLastConnectionAt() {
    return lastConnectionAt;
  }

  public void setLastConnectionAt(@Nullable Instant lastConnectionAt) {
    this.lastConnectionAt = lastConnectionAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CredentialJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

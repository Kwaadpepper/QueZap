package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.lib.ddd.AggregateRoot;

import org.eclipse.jdt.annotation.Nullable;

public class Credential extends AggregateRoot {

  private final HashedIdentifier hashedIdentifier;
  private final @Nullable ZonedDateTime lastConnectionAt;

  private HashedPassword hashedPassword;
  private ZonedDateTime updatedAt;

  public Credential(
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable ZonedDateTime lastConnectionAt,
      ZonedDateTime updatedAt) {
    super();
    this.hashedPassword = hashedPassword;
    this.hashedIdentifier = hashedIdentifier;
    this.lastConnectionAt = lastConnectionAt;
    this.updatedAt = updatedAt;
  }

  protected Credential(
      UUID id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable ZonedDateTime lastConnectionAt,
      ZonedDateTime updatedAt) {
    super(id);
    this.hashedPassword = hashedPassword;
    this.hashedIdentifier = hashedIdentifier;
    this.lastConnectionAt = lastConnectionAt;
    this.updatedAt = updatedAt;
  }

  public static Credential hydrate(
      UUID id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable ZonedDateTime lastConnectionAt,
      ZonedDateTime updatedAt) {
    return new Credential(id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public CredentialId getCredentialId() {
    return new CredentialId(id);
  }

  public @Nullable ZonedDateTime getLastConnectionAt() {
    return lastConnectionAt;
  }

  public HashedIdentifier getHashedIdentifier() {
    return hashedIdentifier;
  }

  public HashedPassword getHashedPassword() {
    return hashedPassword;
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void updatePassword(HashedPassword newHashedPassword) {
    this.hashedPassword = newHashedPassword;
    this.updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Credential that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

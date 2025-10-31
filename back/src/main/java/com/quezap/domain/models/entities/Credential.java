package com.quezap.domain.models.entities;

import java.util.Objects;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.TracksUpdatedAt;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

import org.jspecify.annotations.Nullable;

public class Credential extends AggregateRoot<CredentialId> implements TracksUpdatedAt {

  private final HashedIdentifier hashedIdentifier;
  private final @Nullable TimelinePoint lastConnectionAt;

  private HashedPassword hashedPassword;
  private TimelinePoint updatedAt;

  public Credential(HashedPassword hashedPassword, HashedIdentifier hashedIdentifier) {
    super();
    this.hashedPassword = hashedPassword;
    this.hashedIdentifier = hashedIdentifier;
    this.lastConnectionAt = null;
    this.updatedAt = TimelinePoint.now();
  }

  protected Credential(
      UUID id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable TimelinePoint lastConnectionAt,
      TimelinePoint updatedAt) {
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
      @Nullable TimelinePoint lastConnectionAt,
      TimelinePoint updatedAt) {
    return new Credential(id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt);
  }

  @Override
  public CredentialId getId() {
    return new CredentialId(rawId);
  }

  public @Nullable TimelinePoint getLastConnectionAt() {
    return lastConnectionAt;
  }

  public HashedIdentifier getHashedIdentifier() {
    return hashedIdentifier;
  }

  public HashedPassword getHashedPassword() {
    return hashedPassword;
  }

  @Override
  public TimelinePoint getCreatedAt() {
    return createdAt;
  }

  @Override
  public TimelinePoint getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public void setUpdateAt(TimelinePoint now) {
    this.updatedAt = now;
  }

  public void updatePassword(HashedPassword newHashedPassword) {
    this.hashedPassword = newHashedPassword;
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
    return Objects.hash(rawId);
  }
}

package com.quezap.domain.models.entities;

import java.util.Objects;

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
  private boolean activated;
  private TimelinePoint updatedAt;

  public Credential(HashedPassword hashedPassword, HashedIdentifier hashedIdentifier) {
    this(hashedPassword, hashedIdentifier, true);
  }

  public Credential(
      HashedPassword hashedPassword, HashedIdentifier hashedIdentifier, boolean activated) {
    super();
    this.hashedPassword = hashedPassword;
    this.hashedIdentifier = hashedIdentifier;
    this.lastConnectionAt = null;
    this.activated = activated;
    this.updatedAt = TimelinePoint.now();
  }

  protected Credential(
      CredentialId id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable TimelinePoint lastConnectionAt,
      TimelinePoint updatedAt,
      boolean activated) {
    super(id.value());
    this.hashedPassword = hashedPassword;
    this.hashedIdentifier = hashedIdentifier;
    this.lastConnectionAt = lastConnectionAt;
    this.activated = activated;
    this.updatedAt = updatedAt;
  }

  public static Credential hydrate(
      CredentialId id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable TimelinePoint lastConnectionAt,
      TimelinePoint updatedAt) {
    return new Credential(id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt, true);
  }

  public static Credential hydrate(
      CredentialId id,
      HashedPassword hashedPassword,
      HashedIdentifier hashedIdentifier,
      @Nullable TimelinePoint lastConnectionAt,
      TimelinePoint updatedAt,
      boolean activated) {
    return new Credential(
        id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt, activated);
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

  public boolean isActivated() {
    return activated;
  }

  public void activate() {
    this.activated = true;
  }

  public void deactivate() {
    this.activated = false;
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
    return Objects.hashCode(rawId);
  }
}

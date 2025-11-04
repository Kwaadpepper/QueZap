package com.quezap.domain.models.entities;

import java.util.Objects;

import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.TracksUpdatedAt;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public class User extends AggregateRoot<UserId> implements TracksUpdatedAt {
  private String name;

  private CredentialId credential;
  private TimelinePoint updatedAt;

  private static void validateCommonInvariants(String name) {
    Domain.checkDomain(() -> !name.isBlank(), "Name cannot be blank");
    Domain.checkDomain(() -> name.trim().length() >= 3, "Name cannot be less than 3 characters");
    Domain.checkDomain(() -> name.length() <= 65, "Name cannot exceed 65 characters");
  }

  public User(String name, CredentialId credential) {
    validateCommonInvariants(name);
    this.name = name;
    this.credential = credential;
    this.updatedAt = TimelinePoint.now();
  }

  protected User(UserId id, String name, CredentialId credential, TimelinePoint updatedAt) {
    super(id.value());
    validateCommonInvariants(name);
    this.name = name;
    this.credential = credential;
    this.updatedAt = updatedAt;
  }

  public static User hydrate(
      UserId id, String name, CredentialId credential, TimelinePoint updatedAt) {
    return new User(id, name, credential, updatedAt);
  }

  @Override
  public UserId getId() {
    return new UserId(rawId);
  }

  public String getName() {
    return name;
  }

  public void rename(String name) {
    validateCommonInvariants(name);
    this.name = name;
  }

  public CredentialId getCredential() {
    return credential;
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

  public void setUpdatedAt(TimelinePoint updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof User that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawId);
  }
}

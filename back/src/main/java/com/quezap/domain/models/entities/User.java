package com.quezap.domain.models.entities;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.utils.Domain;

import jakarta.persistence.Entity;
import org.eclipse.jdt.annotation.Nullable;

@Entity
public class User extends AggregateRoot {
  private String name;

  private CredentialId credential;

  private ZonedDateTime updatedAt;

  private static void validateCommonInvariants(String name) {
    Domain.checkDomain(() -> !name.isBlank(), "Name cannot be blank");
    Domain.checkDomain(() -> name.trim().length() >= 3, "Name cannot be less than 3 characters");
    Domain.checkDomain(() -> name.length() <= 65, "Name cannot exceed 65 characters");
  }

  public User(String name, CredentialId credential, ZonedDateTime updatedAt) {
    validateCommonInvariants(name);
    this.name = name;
    this.credential = credential;
    this.updatedAt = updatedAt;
  }

  protected User(UUID id, String name, CredentialId credential, ZonedDateTime updatedAt) {
    super(id);
    validateCommonInvariants(name);
    this.name = name;
    this.credential = credential;
    this.updatedAt = updatedAt;
  }

  public static User hydrate(
      UUID id, String name, CredentialId credential, ZonedDateTime updatedAt) {
    return new User(id, name, credential, updatedAt);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CredentialId getCredential() {
    return credential;
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(ZonedDateTime updatedAt) {
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
    return Objects.hash(id);
  }
}

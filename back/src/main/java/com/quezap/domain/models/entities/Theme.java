package com.quezap.domain.models.entities;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.utils.Domain;

import org.eclipse.jdt.annotation.Nullable;

public class Theme extends AggregateRoot {
  private final String value;

  private static void validateCommonInvariants(String theme) {
    Domain.checkDomain(() -> !theme.isBlank(), "Theme cannot be blank");
    Domain.checkDomain(() -> theme.trim().length() >= 2, "Theme cannot be less than 2 characters");
    Domain.checkDomain(() -> theme.length() <= 100, "Theme cannot exceed 100 characters");
  }

  public Theme(String value) {
    super();
    validateCommonInvariants(value);
    this.value = value;
  }

  protected Theme(UUID id, String value) {
    super(id);
    validateCommonInvariants(value);
    this.value = value;
  }

  public static Theme hydrate(UUID id, String value) {
    return new Theme(id, value);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  @Override
  public ZonedDateTime getCreatedAt() {
    return createdAt;
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

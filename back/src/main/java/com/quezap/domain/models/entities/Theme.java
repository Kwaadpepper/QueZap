package com.quezap.domain.models.entities;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.lib.ddd.AggregateRoot;

import org.jspecify.annotations.Nullable;

public class Theme extends AggregateRoot {
  private ThemeName value;

  public Theme(ThemeName name) {
    super();
    this.value = name;
  }

  protected Theme(UUID id, ThemeName name) {
    super(id);
    this.value = name;
  }

  public static Theme hydrate(UUID id, ThemeName name) {
    return new Theme(id, name);
  }

  @Override
  public UUID getId() {
    return id;
  }

  public ThemeName getName() {
    return value;
  }

  public void setName(ThemeName name) {
    this.value = name;
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

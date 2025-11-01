package com.quezap.domain.models.entities;

import java.util.Objects;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

import org.jspecify.annotations.Nullable;

public class Theme extends AggregateRoot<ThemeId> {
  private ThemeName value;

  public Theme(ThemeName name) {
    super();
    this.value = name;
  }

  protected Theme(ThemeId id, ThemeName name) {
    super(id.value());
    this.value = name;
  }

  public static Theme hydrate(ThemeId id, ThemeName name) {
    return new Theme(id, name);
  }

  @Override
  public ThemeId getId() {
    return new ThemeId(rawId);
  }

  public ThemeName getName() {
    return value;
  }

  public void setName(ThemeName name) {
    this.value = name;
  }

  @Override
  public TimelinePoint getCreatedAt() {
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
    if (!(obj instanceof Theme that)) {
      return false;
    }
    return getId().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawId);
  }
}

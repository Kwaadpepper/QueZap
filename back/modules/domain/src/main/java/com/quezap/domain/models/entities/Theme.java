package com.quezap.domain.models.entities;

import java.util.Objects;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

import org.jspecify.annotations.Nullable;

public class Theme extends AggregateRoot<ThemeId> {
  private ThemeName value;
  private final @Nullable UserId createdBy;

  public Theme(ThemeName name) {
    this(name, null);
  }

  public Theme(ThemeName name, @Nullable UserId createdBy) {
    super();
    this.value = name;
    this.createdBy = createdBy;
  }

  protected Theme(ThemeId id, ThemeName name) {
    this(id, name, null);
  }

  protected Theme(ThemeId id, ThemeName name, @Nullable UserId createdBy) {
    super(id.value());
    this.value = name;
    this.createdBy = createdBy;
  }

  public static Theme hydrate(ThemeId id, ThemeName name) {
    return hydrate(id, name, null);
  }

  public static Theme hydrate(ThemeId id, ThemeName name, @Nullable UserId createdBy) {
    return new Theme(id, name, createdBy);
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

  public @Nullable UserId getCreatedBy() {
    return createdBy;
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
    return Objects.hashCode(rawId);
  }
}

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
    name = "themes",
    indexes = {
      @Index(name = "idx_themes_name", columnList = "name", unique = true),
      @Index(name = "idx_themes_created_by", columnList = "created_by")
    })
public class ThemeJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100, unique = true)
  private String name;

  @Column(name = "created_by")
  private @Nullable UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected ThemeJpaEntity() {
    // For JPA
  }

  public ThemeJpaEntity(UUID id, String name, Instant createdAt) {
    this(id, name, createdAt, null);
  }

  public ThemeJpaEntity(UUID id, String name, Instant createdAt, @Nullable UUID createdBy) {
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ThemeJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}

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
    name = "staged_pictures",
    indexes = {
      @Index(name = "idx_staged_pictures_expires_at", columnList = "expires_at"),
      @Index(name = "idx_staged_pictures_object_key", columnList = "object_key", unique = true),
      @Index(name = "idx_staged_pictures_picture_url", columnList = "picture_url", unique = true)
    })
public class StagedPictureJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "object_key", nullable = false, length = 255, unique = true)
  private String objectKey;

  @Column(name = "picture_url", nullable = false, length = 255, unique = true)
  private String pictureUrl;

  @Column(name = "uploader_id", nullable = false)
  private UUID uploaderId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @SuppressWarnings("NullAway.Init")
  protected StagedPictureJpaEntity() {
    // For JPA
  }

  public StagedPictureJpaEntity(
      UUID id,
      String objectKey,
      String pictureUrl,
      UUID uploaderId,
      Instant createdAt,
      Instant expiresAt) {
    this.id = id;
    this.objectKey = objectKey;
    this.pictureUrl = pictureUrl;
    this.uploaderId = uploaderId;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }

  public String getPictureUrl() {
    return pictureUrl;
  }

  public void setPictureUrl(String pictureUrl) {
    this.pictureUrl = pictureUrl;
  }

  public UUID getUploaderId() {
    return uploaderId;
  }

  public void setUploaderId(UUID uploaderId) {
    this.uploaderId = uploaderId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StagedPictureJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}

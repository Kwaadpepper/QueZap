package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "users",
    indexes = {@Index(name = "idx_users_name", columnList = "name", unique = true)})
public class UserJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 65, unique = true)
  private String name;

  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "credential_id", nullable = false, unique = true)
  private CredentialJpaEntity credential;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @SuppressWarnings("NullAway.Init")
  protected UserJpaEntity() {
    // For JPA
  }

  public UserJpaEntity(
      UUID id, String name, CredentialJpaEntity credential, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.name = name;
    this.credential = credential;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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

  public CredentialJpaEntity getCredential() {
    return credential;
  }

  public void setCredential(CredentialJpaEntity credential) {
    this.credential = credential;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

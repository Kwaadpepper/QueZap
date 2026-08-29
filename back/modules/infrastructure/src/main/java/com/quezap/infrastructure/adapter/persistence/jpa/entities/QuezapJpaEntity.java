package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "quezaps",
    indexes = {
      @Index(name = "idx_quezaps_author_id", columnList = "author_id"),
      @Index(name = "idx_quezaps_is_public", columnList = "is_public"),
      @Index(name = "idx_quezaps_title", columnList = "title")
    })
public class QuezapJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private @Nullable String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private UserJpaEntity author;

  @Column(name = "is_public", nullable = false)
  private Boolean isPublic = false;

  @OneToMany(
      mappedBy = "quezap",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("questionOrder ASC")
  private Set<QuezapQuestionJpaEntity> questions = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @SuppressWarnings("NullAway.Init")
  protected QuezapJpaEntity() {
    // For JPA
  }

  public QuezapJpaEntity(
      UUID id,
      String title,
      @Nullable String description,
      UserJpaEntity author,
      Boolean isPublic,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.author = author;
    this.isPublic = isPublic;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public UserJpaEntity getAuthor() {
    return author;
  }

  public void setAuthor(UserJpaEntity author) {
    this.author = author;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public Set<QuezapQuestionJpaEntity> getQuestions() {
    return questions;
  }

  public void setQuestions(Set<QuezapQuestionJpaEntity> questions) {
    this.questions = questions;
  }

  public void addQuestion(QuezapQuestionJpaEntity question) {
    this.questions.add(question);
    question.setQuezap(this);
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
    if (!(o instanceof QuezapJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

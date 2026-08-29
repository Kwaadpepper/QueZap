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
    name = "questions",
    indexes = {
      @Index(name = "idx_questions_theme_id", columnList = "theme_id"),
      @Index(name = "idx_questions_created_at", columnList = "created_at")
    })
public class QuestionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "type", nullable = false, length = 20)
  private String type;

  @Column(name = "label", nullable = false, length = 255)
  private String label;

  @Column(name = "picture_filename", length = 255)
  private @Nullable String pictureFilename;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "theme_id", nullable = false)
  private ThemeJpaEntity theme;

  @OneToMany(
      mappedBy = "question",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("answerOrder ASC")
  private Set<AnswerJpaEntity> answers = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @SuppressWarnings("NullAway.Init")
  protected QuestionJpaEntity() {
    // For JPA
  }

  public QuestionJpaEntity(
      UUID id,
      String type,
      String label,
      @Nullable String pictureFilename,
      ThemeJpaEntity theme,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.type = type;
    this.label = label;
    this.pictureFilename = pictureFilename;
    this.theme = theme;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public @Nullable String getPictureFilename() {
    return pictureFilename;
  }

  public void setPictureFilename(@Nullable String pictureFilename) {
    this.pictureFilename = pictureFilename;
  }

  public ThemeJpaEntity getTheme() {
    return theme;
  }

  public void setTheme(ThemeJpaEntity theme) {
    this.theme = theme;
  }

  public Set<AnswerJpaEntity> getAnswers() {
    return answers;
  }

  public void setAnswers(Set<AnswerJpaEntity> answers) {
    this.answers = answers;
  }

  public void addAnswer(AnswerJpaEntity answer) {
    this.answers.add(answer);
    answer.setQuestion(this);
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
    if (!(o instanceof QuestionJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

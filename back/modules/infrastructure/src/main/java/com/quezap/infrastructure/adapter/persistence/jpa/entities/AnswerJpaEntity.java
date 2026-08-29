package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "answers",
    indexes = {@Index(name = "idx_answers_question_id", columnList = "question_id")})
public class AnswerJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private QuestionJpaEntity question;

  @Column(name = "label", length = 255)
  private @Nullable String label;

  @Column(name = "picture_filename", length = 255)
  private @Nullable String pictureFilename;

  @Column(name = "is_correct", nullable = false)
  private Boolean isCorrect = false;

  @Column(name = "answer_order", nullable = false)
  private Integer answerOrder;

  @SuppressWarnings("NullAway.Init")
  protected AnswerJpaEntity() {
    // For JPA
  }

  public AnswerJpaEntity(
      UUID id,
      QuestionJpaEntity question,
      @Nullable String label,
      @Nullable String pictureFilename,
      Boolean isCorrect,
      Integer answerOrder) {
    this.id = id;
    this.question = question;
    this.label = label;
    this.pictureFilename = pictureFilename;
    this.isCorrect = isCorrect;
    this.answerOrder = answerOrder;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public QuestionJpaEntity getQuestion() {
    return question;
  }

  public void setQuestion(QuestionJpaEntity question) {
    this.question = question;
  }

  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public @Nullable String getPictureFilename() {
    return pictureFilename;
  }

  public void setPictureFilename(@Nullable String pictureFilename) {
    this.pictureFilename = pictureFilename;
  }

  public Boolean getIsCorrect() {
    return isCorrect;
  }

  public void setIsCorrect(Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public Integer getAnswerOrder() {
    return answerOrder;
  }

  public void setAnswerOrder(Integer answerOrder) {
    this.answerOrder = answerOrder;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnswerJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

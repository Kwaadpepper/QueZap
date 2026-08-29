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
    name = "quezap_questions",
    indexes = {
      @Index(name = "idx_qq_quezap_id", columnList = "quezap_id"),
      @Index(name = "idx_qq_question_id", columnList = "question_id")
    })
public class QuezapQuestionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quezap_id", nullable = false)
  private QuezapJpaEntity quezap;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private QuestionJpaEntity question;

  @Column(name = "timer_seconds", nullable = false)
  private Integer timerSeconds = 30;

  @Column(name = "points", nullable = false)
  private Integer points = 100;

  @Column(name = "question_order", nullable = false)
  private Integer questionOrder;

  @SuppressWarnings("NullAway.Init")
  protected QuezapQuestionJpaEntity() {
    // For JPA
  }

  public QuezapQuestionJpaEntity(
      UUID id,
      QuezapJpaEntity quezap,
      QuestionJpaEntity question,
      Integer timerSeconds,
      Integer points,
      Integer questionOrder) {
    this.id = id;
    this.quezap = quezap;
    this.question = question;
    this.timerSeconds = timerSeconds;
    this.points = points;
    this.questionOrder = questionOrder;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public QuezapJpaEntity getQuezap() {
    return quezap;
  }

  public void setQuezap(QuezapJpaEntity quezap) {
    this.quezap = quezap;
  }

  public QuestionJpaEntity getQuestion() {
    return question;
  }

  public void setQuestion(QuestionJpaEntity question) {
    this.question = question;
  }

  public Integer getTimerSeconds() {
    return timerSeconds;
  }

  public void setTimerSeconds(Integer timerSeconds) {
    this.timerSeconds = timerSeconds;
  }

  public Integer getPoints() {
    return points;
  }

  public void setPoints(Integer points) {
    this.points = points;
  }

  public Integer getQuestionOrder() {
    return questionOrder;
  }

  public void setQuestionOrder(Integer questionOrder) {
    this.questionOrder = questionOrder;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof QuezapQuestionJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

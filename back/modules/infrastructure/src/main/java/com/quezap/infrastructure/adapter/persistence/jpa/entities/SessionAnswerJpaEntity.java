package com.quezap.infrastructure.adapter.persistence.jpa.entities;

import java.time.Instant;
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
import jakarta.persistence.UniqueConstraint;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "session_answers",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_session_participant_slide",
          columnNames = {"session_id", "participant_name", "slide_index"})
    },
    indexes = {
      @Index(name = "idx_sa_session_slide", columnList = "session_id, slide_index"),
      @Index(name = "idx_sa_session_participant", columnList = "session_id, participant_name")
    })
public class SessionAnswerJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private SessionJpaEntity session;

  @Column(name = "participant_name", nullable = false, length = 65)
  private String participantName;

  @Column(name = "slide_index", nullable = false)
  private Integer slideIndex;

  @Column(name = "answer_index", nullable = false)
  private Integer answerIndex = 0;

  @Column(name = "text_answer")
  private @Nullable String textAnswer;

  @Column(name = "points_awarded", nullable = false)
  private Integer pointsAwarded = 0;

  @Column(name = "is_correct", nullable = false)
  private Boolean isCorrect = false;

  @Column(name = "answered_at", nullable = false)
  private Instant answeredAt;

  @SuppressWarnings("NullAway.Init")
  protected SessionAnswerJpaEntity() {
    // For JPA
  }

  public SessionAnswerJpaEntity(
      UUID id,
      SessionJpaEntity session,
      String participantName,
      Integer slideIndex,
      Integer answerIndex,
      Integer pointsAwarded,
      Boolean isCorrect,
      Instant answeredAt) {
    this(
        id,
        session,
        participantName,
        slideIndex,
        answerIndex,
        null,
        pointsAwarded,
        isCorrect,
        answeredAt);
  }

  public SessionAnswerJpaEntity(
      UUID id,
      SessionJpaEntity session,
      String participantName,
      Integer slideIndex,
      @Nullable Integer answerIndex,
      @Nullable String textAnswer,
      Integer pointsAwarded,
      Boolean isCorrect,
      Instant answeredAt) {
    this.id = id;
    this.session = session;
    this.participantName = participantName;
    this.slideIndex = slideIndex;
    this.answerIndex = answerIndex != null ? answerIndex : 0;
    this.textAnswer = textAnswer;
    this.pointsAwarded = pointsAwarded;
    this.isCorrect = isCorrect;
    this.answeredAt = answeredAt;
  }

  public @Nullable String getTextAnswer() {
    return textAnswer;
  }

  public void setTextAnswer(@Nullable String textAnswer) {
    this.textAnswer = textAnswer;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public SessionJpaEntity getSession() {
    return session;
  }

  public void setSession(SessionJpaEntity session) {
    this.session = session;
  }

  public String getParticipantName() {
    return participantName;
  }

  public void setParticipantName(String participantName) {
    this.participantName = participantName;
  }

  public Integer getSlideIndex() {
    return slideIndex;
  }

  public void setSlideIndex(Integer slideIndex) {
    this.slideIndex = slideIndex;
  }

  public Integer getAnswerIndex() {
    return answerIndex;
  }

  public void setAnswerIndex(Integer answerIndex) {
    this.answerIndex = answerIndex;
  }

  public Integer getPointsAwarded() {
    return pointsAwarded;
  }

  public void setPointsAwarded(Integer pointsAwarded) {
    this.pointsAwarded = pointsAwarded;
  }

  public Boolean getIsCorrect() {
    return isCorrect;
  }

  public void setIsCorrect(Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public Instant getAnsweredAt() {
    return answeredAt;
  }

  public void setAnsweredAt(Instant answeredAt) {
    this.answeredAt = answeredAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SessionAnswerJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

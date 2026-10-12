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
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "sessions",
    indexes = {
      @Index(name = "idx_sessions_number", columnList = "number", unique = true),
      @Index(name = "idx_sessions_author_id", columnList = "author_id")
    })
public class SessionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "number", nullable = false, unique = true)
  private Long number;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "current_slide_index", nullable = false)
  private Integer currentSlideIndex = 0;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quezap_id")
  private @Nullable QuezapJpaEntity quezap;

  @OneToMany(
      mappedBy = "session",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<SessionParticipantJpaEntity> participants = new HashSet<>();

  @OneToMany(
      mappedBy = "session",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<SessionAnswerJpaEntity> answers = new HashSet<>();

  @Column(name = "started_at")
  private @Nullable Instant startedAt;

  @Column(name = "current_slide_started_at")
  private @Nullable Instant currentSlideStartedAt;

  @Column(name = "ended_at")
  private @Nullable Instant endedAt;

  @Column(name = "last_revealed_slide_index")
  private @Nullable Integer lastRevealedSlideIndex = 0;

  @Column(name = "progression_mode", nullable = false, length = 20)
  private String progressionMode = "MANUAL";

  @Column(name = "is_paused", nullable = false)
  private Boolean isPaused = false;

  @Column(name = "explanation_duration_seconds", nullable = false)
  private Integer explanationDurationSeconds = 15;

  @Column(name = "mobile_correction_enabled", nullable = false)
  private Boolean mobileCorrectionEnabled = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @SuppressWarnings("NullAway.Init")
  protected SessionJpaEntity() {
    // For JPA
  }

  public SessionJpaEntity(
      UUID id,
      Long number,
      String name,
      Integer currentSlideIndex,
      UUID authorId,
      @Nullable QuezapJpaEntity quezap,
      @Nullable Instant startedAt,
      @Nullable Instant endedAt,
      Instant createdAt) {
    this.id = id;
    this.number = number;
    this.name = name;
    this.currentSlideIndex = currentSlideIndex;
    this.authorId = authorId;
    this.quezap = quezap;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Long getNumber() {
    return number;
  }

  public void setNumber(Long number) {
    this.number = number;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getCurrentSlideIndex() {
    return currentSlideIndex;
  }

  public void setCurrentSlideIndex(Integer currentSlideIndex) {
    this.currentSlideIndex = currentSlideIndex;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public void setAuthorId(UUID authorId) {
    this.authorId = authorId;
  }

  public @Nullable QuezapJpaEntity getQuezap() {
    return quezap;
  }

  public void setQuezap(@Nullable QuezapJpaEntity quezap) {
    this.quezap = quezap;
  }

  public Set<SessionParticipantJpaEntity> getParticipants() {
    return participants;
  }

  public void setParticipants(Set<SessionParticipantJpaEntity> participants) {
    this.participants = participants;
  }

  public void addParticipant(SessionParticipantJpaEntity participant) {
    this.participants.add(participant);
    participant.setSession(this);
  }

  public Set<SessionAnswerJpaEntity> getAnswers() {
    return answers;
  }

  public void setAnswers(Set<SessionAnswerJpaEntity> answers) {
    this.answers = answers;
  }

  public void addAnswer(SessionAnswerJpaEntity answer) {
    this.answers.add(answer);
    answer.setSession(this);
  }

  public @Nullable Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(@Nullable Instant startedAt) {
    this.startedAt = startedAt;
  }

  public @Nullable Instant getCurrentSlideStartedAt() {
    return currentSlideStartedAt;
  }

  public void setCurrentSlideStartedAt(@Nullable Instant currentSlideStartedAt) {
    this.currentSlideStartedAt = currentSlideStartedAt;
  }

  public @Nullable Instant getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(@Nullable Instant endedAt) {
    this.endedAt = endedAt;
  }

  public @Nullable Integer getLastRevealedSlideIndex() {
    return lastRevealedSlideIndex;
  }

  public void setLastRevealedSlideIndex(@Nullable Integer lastRevealedSlideIndex) {
    this.lastRevealedSlideIndex = lastRevealedSlideIndex;
  }

  public String getProgressionMode() {
    return progressionMode;
  }

  public void setProgressionMode(String progressionMode) {
    this.progressionMode = progressionMode;
  }

  public Boolean getIsPaused() {
    return isPaused;
  }

  public void setIsPaused(Boolean isPaused) {
    this.isPaused = isPaused;
  }

  public Integer getExplanationDurationSeconds() {
    return explanationDurationSeconds;
  }

  public void setExplanationDurationSeconds(Integer explanationDurationSeconds) {
    this.explanationDurationSeconds = explanationDurationSeconds;
  }

  public Boolean getMobileCorrectionEnabled() {
    return mobileCorrectionEnabled;
  }

  public void setMobileCorrectionEnabled(Boolean mobileCorrectionEnabled) {
    this.mobileCorrectionEnabled = mobileCorrectionEnabled;
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
    if (!(o instanceof SessionJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

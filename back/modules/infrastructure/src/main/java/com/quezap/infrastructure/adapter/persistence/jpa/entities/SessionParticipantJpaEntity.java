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
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "session_participants",
    indexes = {
      @Index(name = "idx_sp_session_id", columnList = "session_id"),
      @Index(name = "idx_sp_token", columnList = "participation_token", unique = true)
    })
public class SessionParticipantJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private SessionJpaEntity session;

  @Column(name = "name", nullable = false, length = 65)
  private String name;

  @Column(name = "score", nullable = false)
  private Integer score = 0;

  @Column(name = "participation_token", nullable = false, length = 255, unique = true)
  private String participationToken;

  @Column(name = "is_kicked", nullable = false)
  private Boolean isKicked = false;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  @SuppressWarnings("NullAway.Init")
  protected SessionParticipantJpaEntity() {
    // For JPA
  }

  public SessionParticipantJpaEntity(
      UUID id,
      SessionJpaEntity session,
      String name,
      Integer score,
      String participationToken,
      Boolean isKicked,
      Instant joinedAt) {
    this.id = id;
    this.session = session;
    this.name = name;
    this.score = score;
    this.participationToken = participationToken;
    this.isKicked = isKicked;
    this.joinedAt = joinedAt;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public String getParticipationToken() {
    return participationToken;
  }

  public void setParticipationToken(String participationToken) {
    this.participationToken = participationToken;
  }

  public Boolean getIsKicked() {
    return isKicked;
  }

  public void setIsKicked(Boolean isKicked) {
    this.isKicked = isKicked;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SessionParticipantJpaEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

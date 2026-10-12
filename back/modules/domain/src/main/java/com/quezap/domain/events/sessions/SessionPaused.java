package com.quezap.domain.events.sessions;

import java.time.Instant;

import com.quezap.domain.events.sessions.SessionPaused.SessionPausedPayload;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.events.DomainEvent;

public final class SessionPaused implements DomainEvent<SessionPausedPayload> {
  private final SessionId sessionId;
  private final Instant timestamp;

  public SessionPaused(SessionId sessionId) {
    this.sessionId = sessionId;
    this.timestamp = Instant.now();
  }

  @Override
  public String routingKey() {
    return "session.paused";
  }

  @Override
  public SessionPausedPayload payload() {
    return new SessionPausedPayload(sessionId, timestamp);
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public record SessionPausedPayload(SessionId sessionId, Instant pausedAt) {}
}

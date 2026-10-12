package com.quezap.domain.events.sessions;

import java.time.Instant;

import com.quezap.domain.events.sessions.SessionResumed.SessionResumedPayload;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.events.DomainEvent;

public final class SessionResumed implements DomainEvent<SessionResumedPayload> {
  private final SessionId sessionId;
  private final Instant timestamp;

  public SessionResumed(SessionId sessionId) {
    this.sessionId = sessionId;
    this.timestamp = Instant.now();
  }

  @Override
  public String routingKey() {
    return "session.resumed";
  }

  @Override
  public SessionResumedPayload payload() {
    return new SessionResumedPayload(sessionId, timestamp);
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public record SessionResumedPayload(SessionId sessionId, Instant resumedAt) {}
}

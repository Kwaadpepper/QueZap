package com.quezap.domain.events.sessions;

import java.time.Instant;

import com.quezap.domain.events.sessions.SessionEnded.SessionEndedPayload;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.events.DomainEvent;

public final class SessionEnded implements DomainEvent<SessionEndedPayload> {
  private final SessionId sessionId;
  private final Instant timestamp;

  public SessionEnded(SessionId sessionId) {
    this.sessionId = sessionId;
    this.timestamp = Instant.now();
  }

  @Override
  public String routingKey() {
    return "session.ended";
  }

  @Override
  public SessionEndedPayload payload() {
    return new SessionEndedPayload(sessionId);
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public static record SessionEndedPayload(SessionId sessionId) {}
}

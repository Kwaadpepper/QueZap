package com.quezap.domain.events.sessions;

import java.time.Instant;

import com.quezap.domain.events.sessions.SessionStarted.SessionStartedPayload;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.events.DomainEvent;

public final class SessionStarted implements DomainEvent<SessionStartedPayload> {
  private final SessionId sessionId;
  private final Instant timestamp;

  public SessionStarted(SessionId sessionId) {
    this.sessionId = sessionId;
    this.timestamp = Instant.now();
  }

  @Override
  public String routingKey() {
    return "session.started";
  }

  @Override
  public SessionStartedPayload payload() {
    return new SessionStartedPayload(sessionId);
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public static record SessionStartedPayload(SessionId sessionId) {}
}

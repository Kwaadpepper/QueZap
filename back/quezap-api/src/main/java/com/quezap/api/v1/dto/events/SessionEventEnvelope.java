package com.quezap.api.v1.dto.events;

import java.time.Instant;
import java.util.UUID;

public record SessionEventEnvelope<T>(
    String type, Long sequence, Long sessionNumber, UUID sessionId, Instant timestamp, T payload) {

  public static <T> SessionEventEnvelope<T> of(
      String type, Long sequence, Long sessionNumber, UUID sessionId, T payload) {
    return new SessionEventEnvelope<>(
        type, sequence, sessionNumber, sessionId, Instant.now(), payload);
  }

  public static <T> SessionEventEnvelope<T> of(
      String type, Long sessionNumber, UUID sessionId, T payload) {
    return new SessionEventEnvelope<>(type, 0L, sessionNumber, sessionId, Instant.now(), payload);
  }
}

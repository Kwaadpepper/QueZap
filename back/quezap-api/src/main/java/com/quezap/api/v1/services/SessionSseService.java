package com.quezap.api.v1.services;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.SessionEventEnvelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SessionSseService {

  private static final Logger logger = LoggerFactory.getLogger(SessionSseService.class);
  private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
      new ConcurrentHashMap<>();

  public SseEmitter registerEmitter(UUID sessionId) {
    final var emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
    final var sessionEmitters =
        emitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());

    sessionEmitters.add(emitter);

    emitter.onCompletion(() -> sessionEmitters.remove(emitter));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          sessionEmitters.remove(emitter);
        });
    emitter.onError(
        e -> {
          logger.debug("SSE error for session {}: {}", sessionId, e.getMessage());
          sessionEmitters.remove(emitter);
        });

    try {
      // Send initial connect event to verify stream
      emitter.send(
          SseEmitter.event().data("{\"status\":\"CONNECTED\"}", MediaType.APPLICATION_JSON));
    } catch (IOException e) {
      logger.debug("Failed to send initial connect event to SSE emitter", e);
      sessionEmitters.remove(emitter);
    }

    return emitter;
  }

  public <T> void sendToEmitter(
      SseEmitter emitter, String eventType, Long sessionNumber, UUID sessionId, T payload) {
    try {
      final var envelope = SessionEventEnvelope.of(eventType, sessionNumber, sessionId, payload);
      emitter.send(SseEmitter.event().data(envelope, MediaType.APPLICATION_JSON));
    } catch (IOException e) {
      logger.debug("Failed to send direct event to SSE emitter for session {}", sessionId, e);
    }
  }

  public <T> void broadcast(UUID sessionId, String eventType, Long sessionNumber, T payload) {
    final var sessionEmitters = emitters.get(sessionId);
    if (sessionEmitters == null || sessionEmitters.isEmpty()) {
      return;
    }

    final var envelope = SessionEventEnvelope.of(eventType, sessionNumber, sessionId, payload);
    final var deadEmitters = new CopyOnWriteArrayList<SseEmitter>();

    for (final var emitter : sessionEmitters) {
      try {
        emitter.send(SseEmitter.event().data(envelope, MediaType.APPLICATION_JSON));
      } catch (IOException e) {
        logger.debug("Removing disconnected SSE emitter for session {}", sessionId);
        deadEmitters.add(emitter);
      }
    }

    sessionEmitters.removeAll(deadEmitters);
  }

  public int getActiveEmittersCount(UUID sessionId) {
    final var list = emitters.get(sessionId);
    return list != null ? list.size() : 0;
  }
}

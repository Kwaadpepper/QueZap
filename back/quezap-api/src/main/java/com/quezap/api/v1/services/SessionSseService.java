package com.quezap.api.v1.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.SessionEventEnvelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@EnableScheduling
public class SessionSseService {

  public record StoredEvent(long sequence, String eventType, Long sessionNumber, Object payload) {}

  private static final Logger logger = LoggerFactory.getLogger(SessionSseService.class);
  private static final long DEFAULT_TIMEOUT_MS = 120 * 60 * 1000L; // 2 hours
  private static final int MAX_HISTORY_SIZE = 50;

  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, AtomicLong> sessionSequences = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<StoredEvent>> eventHistory =
      new ConcurrentHashMap<>();

  public long nextSequence(UUID sessionId) {
    return sessionSequences.computeIfAbsent(sessionId, k -> new AtomicLong(0)).incrementAndGet();
  }

  public long getCurrentSequence(UUID sessionId) {
    final var seq = sessionSequences.get(sessionId);
    return seq != null ? seq.get() : 0L;
  }

  public SseEmitter registerEmitter(UUID sessionId) {
    final var emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
    final var sessionEmitters =
        emitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());

    sessionEmitters.add(emitter);

    emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          removeEmitter(sessionId, emitter);
        });
    emitter.onError(
        e -> {
          logger.debug("SSE error for session {}: {}", sessionId, e.getMessage());
          removeEmitter(sessionId, emitter);
        });

    try {
      synchronized (emitter) {
        emitter.send(
            SseEmitter.event()
                .name("CONNECTED")
                .data("{\"status\":\"CONNECTED\"}", MediaType.APPLICATION_JSON));
      }
    } catch (Exception e) {
      logger.debug("Failed to send initial connect event to SSE emitter", e);
      removeEmitter(sessionId, emitter);
    }

    return emitter;
  }

  public <T> void sendToEmitter(
      SseEmitter emitter, String eventType, Long sessionNumber, UUID sessionId, T payload) {
    try {
      final var sequence = getCurrentSequence(sessionId);
      final var envelope =
          SessionEventEnvelope.of(eventType, sequence, sessionNumber, sessionId, payload);
      synchronized (emitter) {
        emitter.send(
            SseEmitter.event()
                .id(String.valueOf(sequence))
                .name(eventType)
                .data(envelope, MediaType.APPLICATION_JSON));
      }
    } catch (Exception e) {
      logger.debug("Failed to send direct event to SSE emitter for session {}", sessionId, e);
    }
  }

  public <T> void broadcast(UUID sessionId, String eventType, Long sessionNumber, T payload) {
    final var sequence = nextSequence(sessionId);
    final var history = eventHistory.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
    history.add(new StoredEvent(sequence, eventType, sessionNumber, payload));
    while (history.size() > MAX_HISTORY_SIZE) {
      history.remove(0);
    }

    final var sessionEmitters = emitters.get(sessionId);
    if (sessionEmitters == null || sessionEmitters.isEmpty()) {
      return;
    }

    final var envelope =
        SessionEventEnvelope.of(eventType, sequence, sessionNumber, sessionId, payload);
    final var deadEmitters = new ArrayList<SseEmitter>();

    for (final var emitter : sessionEmitters) {
      try {
        synchronized (emitter) {
          emitter.send(
              SseEmitter.event()
                  .id(String.valueOf(sequence))
                  .name(eventType)
                  .data(envelope, MediaType.APPLICATION_JSON));
        }
      } catch (Exception e) {
        logger.debug("Removing disconnected SSE emitter for session {}", sessionId);
        deadEmitters.add(emitter);
      }
    }

    if (!deadEmitters.isEmpty()) {
      sessionEmitters.removeAll(deadEmitters);
      if (sessionEmitters.isEmpty()) {
        emitters.remove(sessionId);
      }
    }
  }

  public List<StoredEvent> getEventsSince(UUID sessionId, long lastSequence) {
    final var history = eventHistory.get(sessionId);
    if (history == null) {
      return List.of();
    }
    return history.stream().filter(e -> e.sequence() > lastSequence).toList();
  }

  public void sendStoredEventToEmitter(
      SseEmitter emitter, UUID sessionId, StoredEvent storedEvent) {
    try {
      final var envelope =
          SessionEventEnvelope.of(
              storedEvent.eventType(),
              storedEvent.sequence(),
              storedEvent.sessionNumber(),
              sessionId,
              storedEvent.payload());
      synchronized (emitter) {
        emitter.send(
            SseEmitter.event()
                .id(String.valueOf(storedEvent.sequence()))
                .name(storedEvent.eventType())
                .data(envelope, MediaType.APPLICATION_JSON));
      }
    } catch (Exception e) {
      logger.debug("Failed to replay stored event to SSE emitter for session {}", sessionId, e);
    }
  }

  private void removeEmitter(UUID sessionId, SseEmitter emitter) {
    emitters.computeIfPresent(
        sessionId,
        (k, list) -> {
          list.remove(emitter);
          return list.isEmpty() ? null : list;
        });
  }

  public int getActiveEmittersCount(UUID sessionId) {
    final var list = emitters.get(sessionId);
    return list != null ? list.size() : 0;
  }

  @Scheduled(fixedRate = 15000)
  public void sendKeepAlivePing() {
    emitters.forEach(
        (sessionId, sessionEmitters) -> {
          final var deadEmitters = new ArrayList<SseEmitter>();
          for (final var emitter : sessionEmitters) {
            try {
              synchronized (emitter) {
                emitter.send(SseEmitter.event().comment("keep-alive"));
              }
            } catch (Exception e) {
              logger.debug(
                  "Keep-alive failed for disconnected client on session {}: {}",
                  sessionId,
                  e.getMessage());
              deadEmitters.add(emitter);
            }
          }
          if (!deadEmitters.isEmpty()) {
            sessionEmitters.removeAll(deadEmitters);
          }
        });
  }
}

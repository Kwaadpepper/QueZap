package com.quezap.api.v1.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.SessionEventEnvelope;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SessionSseService {

  public record StoredEvent(long sequence, String eventType, Long sessionNumber, Object payload) {}

  private static final class SessionChannelState {
    private final AtomicLong sequence = new AtomicLong(0);
    private final ArrayDeque<StoredEvent> history = new ArrayDeque<>(MAX_HISTORY_SIZE);
    private Instant lastActivityAt;
    private @Nullable Instant endedAt;

    SessionChannelState(Instant now) {
      this.lastActivityAt = now;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(SessionSseService.class);
  private static final long DEFAULT_TIMEOUT_MS = 120 * 60 * 1000L; // 2 hours
  private static final int MAX_HISTORY_SIZE = 50;
  static final Duration ENDED_SESSION_TTL = Duration.ofMinutes(5);
  static final Duration INACTIVE_SESSION_TTL = Duration.ofHours(4);

  private final Clock clock;
  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, SessionChannelState> channels = new ConcurrentHashMap<>();

  @Autowired
  public SessionSseService() {
    this(Clock.systemUTC());
  }

  public SessionSseService(Clock clock) {
    this.clock = clock;
  }

  public long nextSequence(UUID sessionId) {
    final var now = clock.instant();
    final var state = channels.computeIfAbsent(sessionId, k -> new SessionChannelState(now));
    synchronized (state) {
      state.lastActivityAt = now;
      return state.sequence.incrementAndGet();
    }
  }

  public long getCurrentSequence(UUID sessionId) {
    final var state = channels.get(sessionId);
    return state != null ? state.sequence.get() : 0L;
  }

  public SseEmitter registerEmitter(UUID sessionId) {
    final var now = clock.instant();
    final var state = channels.computeIfAbsent(sessionId, k -> new SessionChannelState(now));
    synchronized (state) {
      state.lastActivityAt = now;
    }

    final var emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
    final var sessionEmitters =
        emitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
    sessionEmitters.add(emitter);

    emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
    emitter.onTimeout(
        () -> {
          removeEmitter(sessionId, emitter);
          completeQuietly(emitter, null);
        });
    emitter.onError(
        e -> {
          logger.debug("SSE error for session {}: {}", sessionId, e.getMessage());
          removeEmitter(sessionId, emitter);
          completeQuietly(emitter, e);
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
      completeQuietly(emitter, e);
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
      removeEmitter(sessionId, emitter);
      completeQuietly(emitter, e);
    }
  }

  public <T> void broadcast(UUID sessionId, String eventType, Long sessionNumber, T payload) {
    final var now = clock.instant();
    final var state = channels.computeIfAbsent(sessionId, k -> new SessionChannelState(now));
    final long sequence;
    synchronized (state) {
      sequence = state.sequence.incrementAndGet();
      state.history.addLast(new StoredEvent(sequence, eventType, sessionNumber, payload));
      while (state.history.size() > MAX_HISTORY_SIZE) {
        state.history.removeFirst();
      }
      state.lastActivityAt = now;
      if ("SESSION_ENDED".equals(eventType)) {
        state.endedAt = now;
      }
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
        completeQuietly(emitter, e);
      }
    }

    removeDeadEmitters(sessionId, sessionEmitters, deadEmitters);
  }

  public List<StoredEvent> getEventsSince(UUID sessionId, long lastSequence) {
    final var state = channels.get(sessionId);
    if (state == null) {
      return List.of();
    }
    synchronized (state) {
      if (state.history.isEmpty()) {
        return List.of();
      }
      final var result = new ArrayList<StoredEvent>(state.history.size());
      for (final var event : state.history) {
        if (event.sequence() > lastSequence) {
          result.add(event);
        }
      }
      return List.copyOf(result);
    }
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
      removeEmitter(sessionId, emitter);
      completeQuietly(emitter, e);
    }
  }

  public void clearSession(UUID sessionId) {
    channels.remove(sessionId);
    final var sessionEmitters = emitters.remove(sessionId);
    if (sessionEmitters != null) {
      for (final var emitter : sessionEmitters) {
        completeQuietly(emitter, null);
      }
    }
  }

  @Scheduled(fixedRate = 60000)
  public void purgeExpiredSessions() {
    purgeExpiredSessions(clock.instant());
  }

  public void purgeExpiredSessions(Instant now) {
    final var expiredIds = new ArrayList<UUID>();
    channels.forEach(
        (sessionId, state) -> {
          synchronized (state) {
            if (state.endedAt != null
                && Duration.between(state.endedAt, now).compareTo(ENDED_SESSION_TTL) > 0) {
              expiredIds.add(sessionId);
            } else if (Duration.between(state.lastActivityAt, now).compareTo(INACTIVE_SESSION_TTL)
                    > 0
                && getActiveEmittersCount(sessionId) == 0) {
              expiredIds.add(sessionId);
            }
          }
        });
    for (final var sessionId : expiredIds) {
      clearSession(sessionId);
    }
  }

  public boolean hasSessionState(UUID sessionId) {
    return channels.containsKey(sessionId);
  }

  private void removeEmitter(UUID sessionId, SseEmitter emitter) {
    emitters.computeIfPresent(
        sessionId,
        (k, list) -> {
          list.remove(emitter);
          return list.isEmpty() ? null : list;
        });
  }

  private void removeDeadEmitters(
      UUID sessionId, CopyOnWriteArrayList<SseEmitter> sessionEmitters, List<SseEmitter> dead) {
    if (dead.isEmpty()) {
      return;
    }
    sessionEmitters.removeAll(dead);
    if (sessionEmitters.isEmpty()) {
      emitters.remove(sessionId, sessionEmitters);
    }
  }

  private static void completeQuietly(SseEmitter emitter, @Nullable Throwable error) {
    try {
      if (error != null) {
        emitter.completeWithError(error);
      } else {
        emitter.complete();
      }
    } catch (Exception _) {
      // Emitter already completed
    }
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
              completeQuietly(emitter, e);
            }
          }
          removeDeadEmitters(sessionId, sessionEmitters, deadEmitters);
        });
  }
}

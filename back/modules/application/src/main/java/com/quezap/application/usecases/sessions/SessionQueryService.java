package com.quezap.application.usecases.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.application.ports.sessions.SessionRealtimePublisher.ParticipantView;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.QuestionRevealEvent;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.QuestionStartEvent;
import com.quezap.application.ports.sessions.SessionRealtimePublisher.SessionEndedEvent;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.SessionStorePort.SessionSummary;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

@Service
public class SessionQueryService {

  public record SessionDetailView(
      UUID id,
      long number,
      String code,
      String name,
      String status,
      int currentSlideIndex,
      int totalSlides,
      int participantsCount,
      List<ParticipantView> participants,
      UUID authorId,
      @Nullable Instant startedAt,
      @Nullable Instant endedAt,
      String progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled) {}

  public record InitialStreamData(
      long sessionNumber,
      String sessionName,
      int totalQuestions,
      List<ParticipantView> participants,
      @Nullable QuestionStartEvent startEvent,
      @Nullable QuestionRevealEvent revealEvent,
      @Nullable List<SessionLeaderboardEntry> leaderboardEntries,
      int leaderboardSlideIndex,
      @Nullable Instant pausedTimestamp,
      @Nullable SessionEndedEvent endedEvent) {}

  private final SessionStorePort sessionStore;

  public SessionQueryService(SessionStorePort sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Transactional(readOnly = true)
  public UUID resolveUserId(String principal) {
    return sessionStore
        .findUserIdByPrincipal(principal)
        .orElseThrow(() -> SessionDomainException.unauthorized("No authenticated user available"));
  }

  @Transactional(readOnly = true)
  public PageOf<SessionSummary> listSessions(
      UUID currentUserId, @Nullable UUID requestedAuthorId, Pagination pagination) {
    if (requestedAuthorId != null && !requestedAuthorId.equals(currentUserId)) {
      throw SessionDomainException.forbidden(
          "Impossible de consulter les sessions d'un autre auteur");
    }
    return sessionStore.findSessionsByAuthorId(currentUserId, pagination);
  }

  @Transactional(readOnly = true)
  public SessionDetailView getById(UUID id) {
    final var session =
        sessionStore
            .findSessionById(id)
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));
    return toDetailView(session);
  }

  @Transactional(readOnly = true)
  public SessionDetailView getByCode(String code) {
    final var sessionNumber = SessionEventAssembler.parseSessionCode(code);
    final var session =
        sessionStore
            .findSessionByNumber(sessionNumber)
            .orElseThrow(() -> SessionDomainException.notFound("Invalid session code: " + code));
    return toDetailView(session);
  }

  @Transactional(readOnly = true)
  public InitialStreamData getInitialStreamData(UUID id) {
    final var session =
        sessionStore
            .findSessionById(id)
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));

    final var participants = SessionEventAssembler.buildActiveParticipantViews(session);
    final var totalQuestions = session.getTotalSlides();
    final var now = Instant.now();

    QuestionStartEvent startEvent = null;
    QuestionRevealEvent revealEvent = null;
    List<SessionLeaderboardEntry> leaderboardEntries = null;
    var leaderboardSlideIndex = 0;
    final var pausedTimestamp =
        session.isPaused()
            ? (session.getPausedAt() != null ? session.getPausedAt().position() : now)
            : null;
    SessionEndedEvent endedEvent = null;

    if (session.getEndedAt() != null) {
      endedEvent =
          SessionEventAssembler.buildSessionEndedEvent(session, session.getEndedAt().position());
    } else if (session.getStartedAt() != null && session.getCurrentSlideIndex() > 0) {
      final var slideIndex = session.getCurrentSlideIndex();
      final var slides = session.getSlideSpecs();
      if (!slides.isEmpty() && slideIndex <= slides.size()) {
        final var answeredCount = SessionEventAssembler.countActiveAnswersForCurrentSlide(session);
        startEvent =
            SessionEventAssembler.buildQuestionStartEvent(session, slideIndex, answeredCount, now);

        if (session.getLastRevealedSlideIndex() != null
            && session.getLastRevealedSlideIndex().equals(slideIndex)) {
          revealEvent = SessionEventAssembler.buildQuestionRevealEvent(session, slideIndex);
          leaderboardEntries = session.buildLeaderboardEntries(slideIndex);
          leaderboardSlideIndex = slideIndex;
        }
      }
    }

    return new InitialStreamData(
        session.getNumber().value(),
        session.getRawName(),
        totalQuestions,
        participants,
        startEvent,
        revealEvent,
        leaderboardEntries,
        leaderboardSlideIndex,
        pausedTimestamp,
        endedEvent);
  }

  private static SessionDetailView toDetailView(Session session) {
    final var participants = SessionEventAssembler.buildActiveParticipantViews(session);
    return new SessionDetailView(
        session.getId().value(),
        session.getNumber().value(),
        String.valueOf(session.getNumber().value()),
        session.getRawName(),
        session.getStatus(),
        session.getCurrentSlideIndex(),
        session.getTotalSlides(),
        participants.size(),
        participants,
        session.getAuthor().value(),
        session.getStartedAt() != null ? session.getStartedAt().position() : null,
        session.getEndedAt() != null ? session.getEndedAt().position() : null,
        session.getProgressionMode().name(),
        session.isPaused(),
        session.getExplanationDurationSeconds(),
        session.isMobileCorrectionEnabled());
  }
}

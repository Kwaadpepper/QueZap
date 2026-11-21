package com.quezap.application.usecases.sessions;

import java.time.Instant;
import java.util.UUID;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.JoinSession.Input;
import com.quezap.application.ports.sessions.JoinSession.JoinSessionUsecase;
import com.quezap.application.ports.sessions.JoinSession.Output;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.ports.services.UserNameSanitizer;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.UuidV7;

@Usecase
public final class JoinSessionHandler implements JoinSessionUsecase {

  public static final int MAX_ACTIVE_PARTICIPANTS = 200;

  private final SessionStorePort sessionStore;
  private final UserNameSanitizer userNameSanitizer;
  private final SessionRealtimePublisher realtimePublisher;

  public JoinSessionHandler(
      SessionStorePort sessionStore,
      UserNameSanitizer userNameSanitizer,
      SessionRealtimePublisher realtimePublisher) {
    this.sessionStore = sessionStore;
    this.userNameSanitizer = userNameSanitizer;
    this.realtimePublisher = realtimePublisher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    if (input.participantName().contains("<") || input.participantName().contains(">")) {
      throw SessionDomainException.badRequest("Participant name must not contain HTML tags");
    }

    final var sessionNumber = SessionEventAssembler.parseSessionCode(input.code());
    final var session =
        sessionStore
            .findSessionByNumber(sessionNumber)
            .orElseThrow(
                () -> SessionDomainException.notFound("Invalid session code: " + input.code()));

    final var sanitizedName = userNameSanitizer.sanitize(input.participantName());
    if (sanitizedName.isBlank()) {
      throw SessionDomainException.badRequest("Invalid participant name");
    }

    final var existingOpt =
        session.getSessionParticipants().stream()
            .filter(p -> p.name().equalsIgnoreCase(sanitizedName))
            .findFirst();

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();
    final var sessionName = session.getRawName();
    final var totalQuestions = session.getTotalSlides();

    if (existingOpt.isPresent()) {
      final var existing = existingOpt.get();
      if (input.existingToken() != null
          && input.existingToken().equals(existing.participationToken())
          && !existing.isKicked()) {
        if (session.getEndedAt() == null) {
          final var participants = SessionEventAssembler.buildActiveParticipantViews(session);
          unitOfWork.onCommit(
              () ->
                  realtimePublisher.publishLobbyState(
                      sessionId, sessionNum, sessionName, totalQuestions, participants));
        }
        return new Output(sessionId, existing.participationToken(), existing.name());
      }
      if (session.getEndedAt() != null) {
        throw SessionDomainException.badRequest("Cette session est déjà terminée");
      }
      throw SessionDomainException.conflict("Participant name is already taken in this session");
    }

    if (session.getEndedAt() != null) {
      throw SessionDomainException.badRequest("Cette session est déjà terminée");
    }

    final var activeParticipantCount =
        session.getSessionParticipants().stream().filter(p -> !p.isKicked()).count();
    if (activeParticipantCount >= MAX_ACTIVE_PARTICIPANTS) {
      throw SessionDomainException.conflict(
          "La session a atteint le nombre maximal de participants");
    }

    final var token = "ptk_" + UUID.randomUUID();
    final var participant =
        new SessionParticipant(
            UuidV7.randomUuid(), sanitizedName, 0, token, false, Instant.now(), 0L, 0, 0L);

    session.addLiveParticipant(participant);
    sessionStore.saveAndFlushSession(session);

    final var participants = SessionEventAssembler.buildActiveParticipantViews(session);
    unitOfWork.onCommit(
        () ->
            realtimePublisher.publishLobbyState(
                sessionId, sessionNum, sessionName, totalQuestions, participants));

    if (session.getStartedAt() != null
        && session.getEndedAt() == null
        && session.getCurrentSlideIndex() > 0) {
      final var currentSlide = session.getCurrentSlideIndex();
      final var totalAnswered = SessionEventAssembler.countActiveAnswersForCurrentSlide(session);
      final var totalActive = participants.size();
      unitOfWork.onCommit(
          () ->
              realtimePublisher.publishAnswerCount(
                  sessionId, sessionNum, currentSlide, totalAnswered, totalActive));
    }

    return new Output(sessionId, token, sanitizedName);
  }
}

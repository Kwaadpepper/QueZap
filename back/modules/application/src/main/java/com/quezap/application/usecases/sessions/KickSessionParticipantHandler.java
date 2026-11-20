package com.quezap.application.usecases.sessions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.KickSessionParticipant.Input;
import com.quezap.application.ports.sessions.KickSessionParticipant.KickSessionParticipantUsecase;
import com.quezap.application.ports.sessions.KickSessionParticipant.Output;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class KickSessionParticipantHandler implements KickSessionParticipantUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public KickSessionParticipantHandler(
      SessionStorePort sessionStore, SessionRealtimePublisher realtimePublisher) {
    this.sessionStore = sessionStore;
    this.realtimePublisher = realtimePublisher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var session =
        sessionStore
            .findSessionById(input.sessionId())
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));

    session.assertHost(input.requesterId());
    final var kicked = session.kickParticipant(input.participantName());
    sessionStore.saveAndFlushSession(session);

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();
    final var sessionName = session.getRawName();
    final var totalQuestions = session.getTotalSlides();
    final var activeParticipants = SessionEventAssembler.buildActiveParticipantViews(session);
    final var isActiveQuestion =
        session.getStartedAt() != null
            && session.getEndedAt() == null
            && session.getCurrentSlideIndex() > 0;
    final var currentSlide = session.getCurrentSlideIndex();
    final var totalAnswered =
        isActiveQuestion ? SessionEventAssembler.countActiveAnswersForCurrentSlide(session) : 0;

    unitOfWork.onCommit(
        () -> {
          realtimePublisher.publishParticipantKicked(
              sessionId, sessionNum, kicked.name(), "HOST_KICK");
          realtimePublisher.publishLobbyState(
              sessionId, sessionNum, sessionName, totalQuestions, activeParticipants);
          if (isActiveQuestion) {
            realtimePublisher.publishAnswerCount(
                sessionId, sessionNum, currentSlide, totalAnswered, activeParticipants.size());
          }
        });

    return new Output();
  }
}

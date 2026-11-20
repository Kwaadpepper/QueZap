package com.quezap.application.usecases.sessions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.LeaveSession.Input;
import com.quezap.application.ports.sessions.LeaveSession.LeaveSessionUsecase;
import com.quezap.application.ports.sessions.LeaveSession.Output;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class LeaveSessionHandler implements LeaveSessionUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public LeaveSessionHandler(
      SessionStorePort sessionStore, SessionRealtimePublisher realtimePublisher) {
    this.sessionStore = sessionStore;
    this.realtimePublisher = realtimePublisher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    if (input.participationToken() == null || input.participationToken().isBlank()) {
      return new Output();
    }

    final var sessionOpt = sessionStore.findSessionById(input.sessionId());
    if (sessionOpt.isEmpty()) {
      return new Output();
    }

    final var session = sessionOpt.get();
    final var removedOpt = session.leaveParticipant(input.participationToken());
    if (removedOpt.isEmpty()) {
      return new Output();
    }

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

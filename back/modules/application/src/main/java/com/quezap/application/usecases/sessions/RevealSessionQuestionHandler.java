package com.quezap.application.usecases.sessions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.RevealSessionQuestion.Input;
import com.quezap.application.ports.sessions.RevealSessionQuestion.Output;
import com.quezap.application.ports.sessions.RevealSessionQuestion.RevealSessionQuestionUsecase;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class RevealSessionQuestionHandler implements RevealSessionQuestionUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public RevealSessionQuestionHandler(
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
    final var slideIndex = session.revealCurrentSlide();
    sessionStore.saveSession(session);

    if (session.getSlideSpecs().isEmpty()) {
      return new Output();
    }

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();
    final var revealEvent = SessionEventAssembler.buildQuestionRevealEvent(session, slideIndex);
    final var leaderboard = session.buildLeaderboardEntries(slideIndex);

    unitOfWork.onCommit(
        () -> {
          if (revealEvent != null) {
            realtimePublisher.publishQuestionReveal(sessionId, sessionNum, revealEvent);
          }
          realtimePublisher.publishIntermediateLeaderboard(
              sessionId, sessionNum, slideIndex, leaderboard);
        });

    return new Output();
  }
}

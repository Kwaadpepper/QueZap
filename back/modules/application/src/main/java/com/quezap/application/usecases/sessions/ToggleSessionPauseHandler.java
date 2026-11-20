package com.quezap.application.usecases.sessions;

import java.time.Instant;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.ToggleSessionPause.Input;
import com.quezap.application.ports.sessions.ToggleSessionPause.Output;
import com.quezap.application.ports.sessions.ToggleSessionPause.ToggleSessionPauseUsecase;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Usecase
public final class ToggleSessionPauseHandler implements ToggleSessionPauseUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public ToggleSessionPauseHandler(
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
    final var now = Instant.now();
    session.setPaused(input.paused(), TimelinePoint.at(now));
    sessionStore.saveAndFlushSession(session);

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();
    final var isPaused = session.isPaused();

    unitOfWork.onCommit(
        () -> realtimePublisher.publishPauseStatus(sessionId, sessionNum, isPaused, now));

    return new Output(sessionId, isPaused);
  }
}

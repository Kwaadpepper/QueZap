package com.quezap.application.usecases.sessions;

import java.time.Instant;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.StartSessionLifecycle.Input;
import com.quezap.application.ports.sessions.StartSessionLifecycle.Output;
import com.quezap.application.ports.sessions.StartSessionLifecycle.StartSessionLifecycleUsecase;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Usecase
public final class StartSessionLifecycleHandler implements StartSessionLifecycleUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public StartSessionLifecycleHandler(
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
    if (session.getStartedAt() != null) {
      throw SessionDomainException.badRequest("Session has already started");
    }

    if (session.getQuezapId() == null && session.getSlideSpecs().isEmpty()) {
      throw SessionDomainException.badRequest(
          "Un Quezap valide est obligatoire pour lancer une session");
    }
    SessionEventAssembler.validateSlidesCompleteness(
        session.getQuezapTitle(), session.getSlideSpecs());

    final var now = Instant.now();
    session.start(TimelinePoint.at(now));
    sessionStore.saveAndFlushSession(session);

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();
    final var totalSlides = session.getSlideSpecs().size();
    final var startedAt = session.getStartedAt() != null ? session.getStartedAt().position() : now;
    final var startEvent = SessionEventAssembler.buildQuestionStartEvent(session, 1, 0, now);

    unitOfWork.onCommit(
        () -> {
          realtimePublisher.publishSessionStarted(sessionId, sessionNum, startedAt, totalSlides, 3);
          realtimePublisher.publishQuestionStart(sessionId, sessionNum, startEvent);
        });

    return new Output("STARTED");
  }
}

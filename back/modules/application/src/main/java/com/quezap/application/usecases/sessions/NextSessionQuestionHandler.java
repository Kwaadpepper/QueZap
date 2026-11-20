package com.quezap.application.usecases.sessions;

import java.time.Instant;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.NextSessionQuestion.Input;
import com.quezap.application.ports.sessions.NextSessionQuestion.NextSessionQuestionUsecase;
import com.quezap.application.ports.sessions.NextSessionQuestion.Output;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Usecase
public final class NextSessionQuestionHandler implements NextSessionQuestionUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public NextSessionQuestionHandler(
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
    final var result = session.advanceToNextSlide(TimelinePoint.at(now));
    sessionStore.saveAndFlushSession(session);

    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();

    if (result.ended()) {
      final var endedEvent = SessionEventAssembler.buildSessionEndedEvent(session, now);
      unitOfWork.onCommit(
          () -> {
            if (result.wasPaused()) {
              realtimePublisher.publishPauseStatus(sessionId, sessionNum, false, now);
            }
            realtimePublisher.publishSessionEnded(sessionId, sessionNum, endedEvent);
          });
      return new Output(result.currentSlideIndex(), result.totalSlides());
    }

    final var startEvent =
        SessionEventAssembler.buildQuestionStartEvent(session, result.currentSlideIndex(), 0, now);
    unitOfWork.onCommit(
        () -> {
          if (result.wasPaused()) {
            realtimePublisher.publishPauseStatus(sessionId, sessionNum, false, now);
          }
          realtimePublisher.publishQuestionStart(sessionId, sessionNum, startEvent);
        });

    return new Output(result.currentSlideIndex(), result.totalSlides());
  }
}

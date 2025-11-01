package com.quezap.application.usecases.sessions;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.StartSession.Input;
import com.quezap.application.ports.sessions.StartSession.Output;
import com.quezap.application.ports.sessions.StartSession.StartSessionUseCase;
import com.quezap.domain.errors.sessions.StartSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class StartSessionHandler implements StartSessionUseCase {
  private final SessionRepository sessionRepository;

  public StartSessionHandler(SessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var sessionId = usecaseInput.id();

    sessionRepository
        .find(sessionId)
        .ifPresentOrElse(
            this::startSession,
            DomainConstraintException.throwWith(StartSessionError.NO_SUCH_SESSION));

    return new Output.Started();
  }

  private void startSession(Session session) {
    session.startSession();
    sessionRepository.persist(session);
  }
}

package com.quezap.application.usecases.sessions;

import com.quezap.domain.errors.sessions.StartSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface StartSession {
  record Input(SessionId id) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record Started() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, StartSession {
    private final SessionRepository sessionRepository;

    public Handler(SessionRepository sessionRepository) {
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
}

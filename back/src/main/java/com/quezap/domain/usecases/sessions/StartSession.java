package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.StartSessionError;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
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
    public Output handle(Input usecaseInput) {
      final var sessionId = usecaseInput.id();
      final var session = sessionRepository.find(sessionId.value());

      if (session == null) {
        throw new DomainConstraintException(StartSessionError.NO_SUCH_SESSION);
      }

      session.startSession();
      sessionRepository.save(session);

      return new Output.Started();
    }
  }
}

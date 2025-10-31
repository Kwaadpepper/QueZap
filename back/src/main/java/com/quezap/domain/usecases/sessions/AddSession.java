package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.AddSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.entities.builders.SessionBuilder;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.utils.EmptyConsumer;

public sealed interface AddSession {
  record Input(SessionName name, UserId user) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record SessionAdded(SessionId id) implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AddSession {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Handler(SessionRepository sessionRepository, UserRepository userRepository) {
      this.sessionRepository = sessionRepository;
      this.userRepository = userRepository;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      final var sessionName = usecaseInput.name();
      final var userId = usecaseInput.user();

      final var newSessionNumber = getNewSessionNumber();
      final var sessionBuilder = SessionBuilder.Builder.with(sessionName, newSessionNumber, userId);
      final var session = sessionBuilder.build();

      userRepository
          .find(userId)
          .ifPresentOrElse(
              EmptyConsumer.accept(),
              DomainConstraintException.throwWith(AddSessionError.NO_SUCH_USER));

      if (sessionRepository.findByNumber(newSessionNumber).isPresent()) {
        throw new IllegalDomainStateException("Generated session code is not unique");
      }

      sessionRepository.persist(session);

      return new Output.SessionAdded(session.getId());
    }

    private SessionNumber getNewSessionNumber() {
      final var lastCreatedSession = sessionRepository.latestByNumber();
      return lastCreatedSession.<SessionNumber>map(Session::getNumber).orElse(new SessionNumber(1));
    }
  }
}

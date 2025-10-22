package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.AddSessionError;
import com.quezap.domain.models.entities.builders.SessionBuilder;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

public sealed interface AddSession {
  record Input(SessionName name, UserId user) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record SessionAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AddSession {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Handler(SessionRepository sessionRepository, UserRepository userRepository) {
      this.sessionRepository = sessionRepository;
      this.userRepository = userRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var sessionName = usecaseInput.name();
      final var userId = usecaseInput.user();

      final var lastCreatedSession = sessionRepository.latestByCode();
      final var sessionNumber = lastCreatedSession.getNumber();
      final var sessionBuilder = SessionBuilder.Builder.with(sessionName, sessionNumber, userId);
      final var session = sessionBuilder.build();

      if (userRepository.find(userId.value()) == null) {
        throw new DomainConstraintException(AddSessionError.NO_SUCH_USER);
      }

      if (sessionRepository.findByNumber(sessionNumber) != null) {
        throw new IllegalDomainStateException("Generated session code is not unique");
      }

      sessionRepository.save(session);

      return new Output.SessionAdded();
    }
  }
}

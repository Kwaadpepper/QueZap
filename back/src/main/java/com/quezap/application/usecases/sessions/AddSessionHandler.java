package com.quezap.application.usecases.sessions;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.AddSession.AddSessionUseCase;
import com.quezap.application.ports.sessions.AddSession.Input;
import com.quezap.application.ports.sessions.AddSession.Output;
import com.quezap.domain.errors.sessions.AddSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.entities.builders.SessionBuilder;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.EmptyConsumer;

@Usecase
public final class AddSessionHandler implements AddSessionUseCase {
  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;

  public AddSessionHandler(SessionRepository sessionRepository, UserRepository userRepository) {
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

package com.quezap.application.usecases.sessions;

import java.security.SecureRandom;
import java.time.Instant;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.CreateSession.CreateSessionUsecase;
import com.quezap.application.ports.sessions.CreateSession.Input;
import com.quezap.application.ports.sessions.CreateSession.Output;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

@Usecase
public final class CreateSessionHandler implements CreateSessionUsecase {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final SessionStorePort sessionStore;

  public CreateSessionHandler(SessionStorePort sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    if (input.quezapId() == null) {
      throw SessionDomainException.badRequest(
          "Un Quezap doit être sélectionné pour créer une session");
    }

    final var quezap =
        sessionStore
            .findQuezapById(input.quezapId())
            .orElseThrow(
                () ->
                    SessionDomainException.badRequest("Quezap introuvable : " + input.quezapId()));

    if (!quezap.authorId().equals(input.authorId()) && !quezap.isPublic()) {
      throw SessionDomainException.forbidden(
          "Impossible de lancer une session sur un questionnaire privé d'un autre auteur");
    }

    SessionEventAssembler.validateSlidesCompleteness(quezap.title(), quezap.slides());

    final var sessionNumber = generateUniqueSessionNumber();
    final var sessionId = UuidV7.randomUuid();
    final var session =
        Session.createLive(
            new SessionId(sessionId),
            input.name(),
            new SessionNumber(sessionNumber),
            new UserId(input.authorId()),
            quezap.id(),
            quezap.title(),
            TimelinePoint.at(Instant.now()),
            quezap.slides());

    sessionStore.saveSession(session);
    return new Output(sessionId, sessionNumber, String.valueOf(sessionNumber));
  }

  private long generateUniqueSessionNumber() {
    for (var i = 0; i < 20; i++) {
      final var number = SECURE_RANDOM.nextLong(10_000_000L, 100_000_000L);
      if (!sessionStore.existsSessionByNumber(number)) {
        return number;
      }
    }
    return SECURE_RANDOM.nextLong(100_000_000L, 1_000_000_000L);
  }
}

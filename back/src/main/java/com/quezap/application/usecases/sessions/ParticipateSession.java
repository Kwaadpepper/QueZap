package com.quezap.application.usecases.sessions;

import com.quezap.domain.errors.sessions.ParticipateSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.services.ParticipationTokenGenerator;
import com.quezap.domain.port.services.SessionCodeEncoder;
import com.quezap.domain.port.services.UserNameSanitizer;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface ParticipateSession {
  record Input(ParticipantName name, SessionCode code) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record Participation(ParticipationToken token) implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, ParticipateSession {
    private final SessionRepository sessionRepository;
    private final ParticipationTokenGenerator participationTokenGenerator;
    private final UserNameSanitizer userNameSanitizer;
    private final SessionCodeEncoder sessionCodeEncoder;

    public Handler(
        SessionRepository sessionRepository,
        ParticipationTokenGenerator sessionTokenGenerator,
        UserNameSanitizer userNameSanitizer,
        SessionCodeEncoder sessionCodeEncoder) {
      this.sessionRepository = sessionRepository;
      this.participationTokenGenerator = sessionTokenGenerator;
      this.userNameSanitizer = userNameSanitizer;
      this.sessionCodeEncoder = sessionCodeEncoder;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {

      final var sessionCode = usecaseInput.code();
      final var participantName = usecaseInput.name();
      final var sessionNumber = sessionCodeEncoder.decode(sessionCode);

      return sessionRepository
          .findByNumber(sessionNumber)
          .<Output>map(session -> addParticipantTo(session, participantName))
          .orElseThrow(DomainConstraintException.with(ParticipateSessionError.INVALID_CODE));
    }

    private Output addParticipantTo(Session session, ParticipantName participantName) {

      final var sessionId = session.getId();
      final var participationToken = participationTokenGenerator.generate(sessionId);
      final var sanitizedUserName = sanitize(participantName);
      final var participant = new Participant(sanitizedUserName, 0, participationToken);

      session.addParticipant(participant);
      sessionRepository.persist(session);

      return new Output.Participation(participationToken);
    }

    private ParticipantName sanitize(ParticipantName name) {
      try {
        return new ParticipantName(userNameSanitizer.sanitize(name.value()));
      } catch (IllegalDomainStateException _) {
        throw new DomainConstraintException(ParticipateSessionError.NAME_REFUSED);
      }
    }
  }
}

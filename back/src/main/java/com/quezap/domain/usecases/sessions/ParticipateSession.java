package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.ParticipateSessionError;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.services.ParticipationTokenGenerator;
import com.quezap.domain.port.services.UserNameSanitizer;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

public sealed interface ParticipateSession {
  record Input(ParticipantName name, SessionCode code) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record Participation(ParticipationToken token) implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, ParticipateSession {
    private final SessionRepository sessionRepository;
    private final ParticipationTokenGenerator participationTokenGenerator;
    private final UserNameSanitizer userNameSanitizer;

    public Handler(
        SessionRepository sessionRepository,
        ParticipationTokenGenerator sessionTokenGenerator,
        UserNameSanitizer userNameSanitizer) {
      this.sessionRepository = sessionRepository;
      this.participationTokenGenerator = sessionTokenGenerator;
      this.userNameSanitizer = userNameSanitizer;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var sessionCode = usecaseInput.code();
      final var participantName = usecaseInput.name();
      final var sanitizedUserName = userNameSanitizer.sanitize(participantName.value());
      final var session = sessionRepository.findByCode(sessionCode);

      if (session == null) {
        throw new DomainConstraintException(ParticipateSessionError.INVALID_CODE);
      }

      final var sessionParticipants = session.getParticipants();

      if (sessionParticipants.stream().anyMatch(p -> p.name() == participantName)) {
        throw new DomainConstraintException(ParticipateSessionError.NAME_ALREADY_TAKEN);
      }

      if (sanitizedUserName.isBlank() || !sanitizedUserName.equals(participantName.value())) {
        throw new DomainConstraintException(ParticipateSessionError.NAME_REFUSED);
      }

      final var sessionId = new SessionId(session.getId());
      final var participationToken = participationTokenGenerator.generate(sessionId);
      final var sessionParticipant = new Participant(participantName, 0, participationToken);

      sessionParticipants.add(sessionParticipant);

      return new Output.Participation(participationToken);
    }
  }
}

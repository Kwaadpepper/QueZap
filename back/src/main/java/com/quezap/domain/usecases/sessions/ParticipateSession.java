package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.ParticipateSessionError;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.services.ParticipationTokenGenerator;
import com.quezap.domain.port.services.SessionCodeEncoder;
import com.quezap.domain.port.services.UserNameSanitizer;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
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
    public Output handle(Input usecaseInput) {
      final var sessionCode = usecaseInput.code();
      final var participantName = usecaseInput.name();
      final var sanitizedUserName = userNameSanitizer.sanitize(participantName.value());

      final var sessionNumber = sessionCodeEncoder.decode(sessionCode);
      final var session = sessionRepository.findByNumber(sessionNumber);

      if (session == null) {
        throw new DomainConstraintException(ParticipateSessionError.INVALID_CODE);
      }

      // * Validate sanitized name
      if (sanitizedUserName.isBlank() || !sanitizedUserName.equals(participantName.value())) {
        throw new DomainConstraintException(ParticipateSessionError.NAME_REFUSED);
      }

      final var sessionId = new SessionId(session.getId());
      final var participationToken = participationTokenGenerator.generate(sessionId);
      final var sessionParticipant = new Participant(participantName, 0, participationToken);

      session.addParticipant(sessionParticipant);

      sessionRepository.save(session);

      return new Output.Participation(participationToken);
    }
  }
}

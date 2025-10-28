package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.AnswerSessionError;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.domain.port.services.SessionCodeEncoder;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.jspecify.annotations.Nullable;

public interface AnswerQuestion {
  record Input(SessionCode code, ParticipationToken token, Integer slideIndex, Integer answerIndex)
      implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record AnswerAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AnswerQuestion {
    private final SessionRepository sessionRepository;
    private final SessionCodeEncoder sessionCodeEncoder;

    public Handler(SessionRepository sessionRepository, SessionCodeEncoder sessionCodeEncoder) {
      this.sessionRepository = sessionRepository;
      this.sessionCodeEncoder = sessionCodeEncoder;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      final var sessionCode = usecaseInput.code();
      final var token = usecaseInput.token();
      final var slideIndex = usecaseInput.slideIndex();
      final var answerIndex = usecaseInput.answerIndex();

      final var sessionNumber = sessionCodeEncoder.decode(sessionCode);
      final var session = sessionRepository.findByNumber(sessionNumber);

      if (session == null) {
        throw new DomainConstraintException(AnswerSessionError.NO_SUCH_SESSION);
      }

      final @Nullable ParticipantName participantName =
          session.getParticipants().stream()
              .filter(p -> p.token().equals(token))
              .map(p -> p.name())
              .findFirst()
              .orElse(null);

      if (participantName == null) {
        throw new DomainConstraintException(AnswerSessionError.INVALID_PARTICIPATION_TOKEN);
      }

      session.addAnswer(participantName, slideIndex, answerIndex);
      sessionRepository.save(session);

      return new Output.AnswerAdded();
    }
  }
}

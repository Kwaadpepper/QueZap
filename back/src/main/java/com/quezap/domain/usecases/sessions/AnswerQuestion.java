package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.AnswerSessionError;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

import org.eclipse.jdt.annotation.Nullable;

public interface AnswerQuestion {
  record Input(SessionCode code, ParticipationToken token, Integer slideIndex, Integer answerIndex)
      implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record AnswerAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AnswerQuestion {
    private final SessionRepository sessionRepository;

    public Handler(SessionRepository sessionRepository) {
      this.sessionRepository = sessionRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var sessionCode = usecaseInput.code();
      final var token = usecaseInput.token();
      final var slideIndex = usecaseInput.slideIndex();
      final var answerIndex = usecaseInput.answerIndex();
      final var session = sessionRepository.findByCode(sessionCode);

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

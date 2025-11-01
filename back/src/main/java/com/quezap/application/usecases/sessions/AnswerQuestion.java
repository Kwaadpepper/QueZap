package com.quezap.application.usecases.sessions;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.quezap.domain.errors.sessions.AnswerSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.domain.ports.services.SessionCodeEncoder;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

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
      final var sessionNumber = sessionCodeEncoder.decode(sessionCode);
      final var session = sessionRepository.findByNumber(sessionNumber);

      session.ifPresentOrElse(
          persistIfValid(usecaseInput),
          DomainConstraintException.throwWith(AnswerSessionError.NO_SUCH_SESSION));

      return new Output.AnswerAdded();
    }

    private Consumer<Session> persistIfValid(Input usecaseInput) {
      final var token = usecaseInput.token();
      final var slideIndex = usecaseInput.slideIndex();
      final var answerIndex = usecaseInput.answerIndex();

      return session -> {
        final var participantName =
            session.getParticipants().stream()
                .filter(whereParticipantHasToken(token))
                .map(Participant::name)
                .findFirst()
                .orElseThrow(
                    DomainConstraintException.with(AnswerSessionError.INVALID_PARTICIPATION_TOKEN));

        session.addAnswer(participantName, slideIndex, answerIndex);
        sessionRepository.persist(session);
      };
    }

    private Predicate<Participant> whereParticipantHasToken(ParticipationToken token) {
      return participant -> participant.token().equals(token);
    }
  }
}

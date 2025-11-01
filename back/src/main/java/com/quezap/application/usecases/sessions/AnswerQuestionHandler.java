package com.quezap.application.usecases.sessions;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.AnswerQuestion.AnswerQuestionUseCase;
import com.quezap.application.ports.sessions.AnswerQuestion.Input;
import com.quezap.application.ports.sessions.AnswerQuestion.Output;
import com.quezap.domain.errors.sessions.AnswerSessionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.domain.ports.services.SessionCodeEncoder;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class AnswerQuestionHandler implements AnswerQuestionUseCase {
  private final SessionRepository sessionRepository;
  private final SessionCodeEncoder sessionCodeEncoder;

  public AnswerQuestionHandler(
      SessionRepository sessionRepository, SessionCodeEncoder sessionCodeEncoder) {
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

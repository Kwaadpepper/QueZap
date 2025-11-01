package com.quezap.application.usecases.sessions;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.RemoveQuestionFromSession.Input;
import com.quezap.application.ports.sessions.RemoveQuestionFromSession.Output;
import com.quezap.application.ports.sessions.RemoveQuestionFromSession.RemoveQuestionFromSessionUsecase;
import com.quezap.domain.errors.sessions.RemoveQuestionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.EmptyConsumer;

@Usecase
public final class RemoveQuestionFromSessionHandler implements RemoveQuestionFromSessionUsecase {
  private final SessionRepository sessionRepository;
  private final QuestionRepository questionRepository;

  public RemoveQuestionFromSessionHandler(
      SessionRepository sessionRepository, QuestionRepository questionRepository) {
    this.sessionRepository = sessionRepository;
    this.questionRepository = questionRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var sessionId = usecaseInput.session();
    final var questionSlide = usecaseInput.question();
    final var questionId = questionSlide.question();

    questionRepository
        .find(questionId)
        .ifPresentOrElse(
            EmptyConsumer.accept(),
            DomainConstraintException.throwWith(RemoveQuestionError.NO_SUCH_QUESTION));

    sessionRepository
        .find(sessionId)
        .ifPresentOrElse(
            session -> removeQuestionFrom(session, questionSlide),
            DomainConstraintException.throwWith(RemoveQuestionError.NO_SUCH_SESSION));

    return new Output.QuestionRemovedFromSession();
  }

  private void removeQuestionFrom(
      Session session,
      com.quezap.domain.models.valueobjects.questions.QuestionSlide questionSlide) {
    session.removeQuestion(questionSlide);
    sessionRepository.persist(session);
  }
}

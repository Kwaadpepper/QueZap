package com.quezap.application.usecases.sessions;

import java.util.function.Consumer;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.sessions.AddQuestionToSession.AddQuestionToSessionUsecase;
import com.quezap.application.ports.sessions.AddQuestionToSession.Input;
import com.quezap.application.ports.sessions.AddQuestionToSession.Output;
import com.quezap.domain.errors.sessions.AddQuestionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.EmptyConsumer;

@Usecase
public final class AddQuestionToSessionHandler implements AddQuestionToSessionUsecase {
  private final SessionRepository sessionRepository;
  private final QuestionRepository questionRepository;

  public AddQuestionToSessionHandler(
      SessionRepository sessionRepository, QuestionRepository questionRepository) {
    this.sessionRepository = sessionRepository;
    this.questionRepository = questionRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var questionSlide = usecaseInput.question();
    final var questionId = questionSlide.question();
    final var sessionId = usecaseInput.session();

    questionRepository
        .find(questionId)
        .ifPresentOrElse(
            EmptyConsumer.accept(),
            DomainConstraintException.throwWith(AddQuestionError.NO_SUCH_QUESTION));

    sessionRepository
        .find(sessionId)
        .ifPresentOrElse(
            persistWith(questionSlide),
            DomainConstraintException.throwWith(AddQuestionError.NO_SUCH_SESSION));

    return new Output.QuestionAddedToSession();
  }

  private Consumer<Session> persistWith(
      com.quezap.domain.models.valueobjects.questions.QuestionSlide questionSlide) {
    return session -> {
      session.addQuestion(questionSlide);
      sessionRepository.persist(session);
    };
  }
}

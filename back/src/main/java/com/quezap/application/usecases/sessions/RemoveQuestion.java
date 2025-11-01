package com.quezap.application.usecases.sessions;

import com.quezap.domain.errors.sessions.RemoveQuestionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.utils.EmptyConsumer;

public sealed interface RemoveQuestion {
  record Input(SessionId session, QuestionSlide question) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record SessionAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, RemoveQuestion {
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    public Handler(SessionRepository sessionRepository, QuestionRepository questionRepository) {
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

      return new Output.SessionAdded();
    }

    private void removeQuestionFrom(Session session, QuestionSlide questionSlide) {
      session.removeQuestion(questionSlide);
      sessionRepository.persist(session);
    }
  }
}

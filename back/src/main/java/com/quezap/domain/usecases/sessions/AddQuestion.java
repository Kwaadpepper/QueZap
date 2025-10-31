package com.quezap.domain.usecases.sessions;

import java.util.function.Consumer;

import com.quezap.domain.errors.sessions.AddQuestionError;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.utils.EmptyConsumer;

public sealed interface AddQuestion {
  record Input(SessionId session, QuestionSlide question) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record QuestionAddedToSession() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AddQuestion {
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    public Handler(SessionRepository sessionRepository, QuestionRepository questionRepository) {
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

    private Consumer<Session> persistWith(QuestionSlide questionSlide) {
      return session -> {
        session.addQuestion(questionSlide);
        sessionRepository.save(session);
      };
    }
  }
}

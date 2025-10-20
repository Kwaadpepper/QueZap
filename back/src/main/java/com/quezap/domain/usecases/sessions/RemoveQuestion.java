package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.RemoveQuestionError;
import com.quezap.domain.models.valueobjects.QuestionSlide;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

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
    public Output handle(Input usecaseInput) {
      final var questionSlide = usecaseInput.question();
      final var questionId = questionSlide.question();
      final var sessionId = usecaseInput.session();
      final var session = sessionRepository.find(sessionId.value());
      final var question = questionRepository.find(questionId.value());

      if (session == null) {
        throw new DomainConstraintException(RemoveQuestionError.NO_SUCH_SESSION);
      }

      if (question == null) {
        throw new DomainConstraintException(RemoveQuestionError.NO_SUCH_QUESTION);
      }

      session.removeQuestion(questionSlide);
      sessionRepository.save(session);

      return new Output.SessionAdded();
    }
  }
}

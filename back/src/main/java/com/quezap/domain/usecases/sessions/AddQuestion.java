package com.quezap.domain.usecases.sessions;

import com.quezap.domain.errors.sessions.AddQuestionError;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.SessionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

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
    public Output handle(Input usecaseInput) {
      final var questionSlide = usecaseInput.question();
      final var questionId = questionSlide.question();
      final var sessionId = usecaseInput.session();
      final var session = sessionRepository.find(sessionId);
      final var question = questionRepository.find(questionId);

      if (session == null) {
        throw new DomainConstraintException(AddQuestionError.NO_SUCH_SESSION);
      }

      if (question == null) {
        throw new DomainConstraintException(AddQuestionError.NO_SUCH_QUESTION);
      }

      session.addQuestion(questionSlide);
      sessionRepository.save(session);

      return new Output.QuestionAddedToSession();
    }
  }
}

package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface AddQuestionToSession {
  record Input(SessionId session, QuestionSlide question) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record QuestionAddedToSession() implements Output {}
  }

  public interface AddQuestionToSessionUseCase
      extends UseCaseHandler<AddQuestionToSession.Input, AddQuestionToSession.Output> {}
}

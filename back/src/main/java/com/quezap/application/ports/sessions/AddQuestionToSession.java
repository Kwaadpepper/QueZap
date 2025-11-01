package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AddQuestionToSession {
  record Input(SessionId session, QuestionSlide question) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record QuestionAddedToSession() implements Output {}
  }

  public interface AddQuestionToSessionUsecase
      extends UsecaseHandler<AddQuestionToSession.Input, AddQuestionToSession.Output> {}
}

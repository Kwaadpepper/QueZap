package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RemoveQuestionFromSession {
  record Input(SessionId session, QuestionSlide question) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record QuestionRemovedFromSession() implements Output {}
  }

  public interface RemoveQuestionFromSessionUsecase
      extends UsecaseHandler<RemoveQuestionFromSession.Input, RemoveQuestionFromSession.Output> {}
}

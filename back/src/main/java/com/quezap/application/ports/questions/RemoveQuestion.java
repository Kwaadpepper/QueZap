package com.quezap.application.ports.questions;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RemoveQuestion {
  record Input(QuestionId questionId) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record QuestionDeleted() implements Output {}
  }

  public interface RemoveQuestionUsecase
      extends UsecaseHandler<RemoveQuestion.Input, RemoveQuestion.Output> {}
}

package com.quezap.application.ports.questions;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface RemoveQuestion {
  record Input(QuestionId questionId) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record QuestionDeleted() implements Output {}
  }

  public interface RemoveQuestionUseCase
      extends UseCaseHandler<RemoveQuestion.Input, RemoveQuestion.Output> {}
}

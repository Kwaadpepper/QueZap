package com.quezap.application.ports.questions;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface RemoveQuestion {
  record Input(QuestionId questionId, @Nullable UserId requesterId, boolean isAdmin)
      implements UsecaseInput {
    public Input(QuestionId questionId) {
      this(questionId, null, false);
    }
  }

  sealed interface Output extends UsecaseOutput {
    record QuestionDeleted() implements Output {}
  }

  class QuestionForbiddenException extends RuntimeException {
    public QuestionForbiddenException(String message) {
      super(message);
    }
  }

  class QuestionInUseException extends RuntimeException {
    public QuestionInUseException(String message) {
      super(message);
    }
  }

  public interface RemoveQuestionUsecase
      extends UsecaseHandler<RemoveQuestion.Input, RemoveQuestion.Output> {}
}

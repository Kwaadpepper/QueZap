package com.quezap.domain.usecases.questions;

import com.quezap.domain.errors.questions.DeleteQuestionError;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface RemoveQuestion {
  record Input(QuestionId questionId) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record QuestionDeleted() implements Output {}
  }

  public static final class Handler implements UseCaseHandler<Input, Output>, RemoveQuestion {
    private final QuestionRepository questionRepository;

    public Handler(QuestionRepository questionRepository) {
      this.questionRepository = questionRepository;
    }

    @Override
    public RemoveQuestion.Output handle(Input input) {
      final var questionId = input.questionId();
      final var question = questionRepository.find(questionId);

      if (question == null) {
        throw new DomainConstraintException(DeleteQuestionError.QUESTION_NOT_FOUND);
      }

      question.delete();

      questionRepository.delete(question);

      return new RemoveQuestion.Output.QuestionDeleted();
    }
  }
}

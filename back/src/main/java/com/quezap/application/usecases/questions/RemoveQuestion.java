package com.quezap.application.usecases.questions;

import com.quezap.domain.errors.questions.DeleteQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
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
    public RemoveQuestion.Output handle(Input input, UnitOfWorkEvents unitOfWork) {
      final var questionId = input.questionId();

      questionRepository
          .find(questionId)
          .ifPresentOrElse(
              this::remove,
              DomainConstraintException.throwWith(DeleteQuestionError.QUESTION_NOT_FOUND));

      return new RemoveQuestion.Output.QuestionDeleted();
    }

    private void remove(Question question) {
      question.delete();
      questionRepository.delete(question);
    }
  }
}

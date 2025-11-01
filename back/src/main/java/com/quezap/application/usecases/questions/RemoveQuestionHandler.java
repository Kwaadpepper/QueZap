package com.quezap.application.usecases.questions;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.questions.RemoveQuestion.Input;
import com.quezap.application.ports.questions.RemoveQuestion.Output;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.errors.questions.DeleteQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class RemoveQuestionHandler implements RemoveQuestionUsecase {
  private final QuestionRepository questionRepository;

  public RemoveQuestionHandler(QuestionRepository questionRepository) {
    this.questionRepository = questionRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var questionId = input.questionId();

    questionRepository
        .find(questionId)
        .ifPresentOrElse(
            this::remove,
            DomainConstraintException.throwWith(DeleteQuestionError.QUESTION_NOT_FOUND));

    return new Output.QuestionDeleted();
  }

  private void remove(Question question) {
    question.delete();
    questionRepository.delete(question);
  }
}

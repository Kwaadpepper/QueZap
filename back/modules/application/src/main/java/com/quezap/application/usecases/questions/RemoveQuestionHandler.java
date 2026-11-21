package com.quezap.application.usecases.questions;

import org.springframework.beans.factory.annotation.Autowired;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.questions.DeleteQuestionError;
import com.quezap.application.ports.questions.RemoveQuestion.Input;
import com.quezap.application.ports.questions.RemoveQuestion.Output;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionForbiddenException;
import com.quezap.application.ports.questions.RemoveQuestion.QuestionInUseException;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

import org.jspecify.annotations.Nullable;

@Usecase
final class RemoveQuestionHandler implements RemoveQuestionUsecase {
  private final QuestionRepository questionRepository;
  private final @Nullable QuezapDataPort quezapDataPort;

  public RemoveQuestionHandler(QuestionRepository questionRepository) {
    this(questionRepository, null);
  }

  @Autowired
  public RemoveQuestionHandler(
      QuestionRepository questionRepository, @Nullable QuezapDataPort quezapDataPort) {
    this.questionRepository = questionRepository;
    this.quezapDataPort = quezapDataPort;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var questionId = input.questionId();

    final var question =
        questionRepository
            .find(questionId)
            .orElseThrow(
                () -> new ApplicationConstraintException(DeleteQuestionError.QUESTION_NOT_FOUND));

    if (!input.isAdmin()) {
      final var requesterId = input.requesterId();
      final var authorId = question.getAuthorId();
      if (requesterId == null || authorId == null || !authorId.equals(requesterId)) {
        throw new QuestionForbiddenException("You are not allowed to delete this question");
      }
    }

    if (quezapDataPort != null
        && quezapDataPort.countQuezapsReferencingQuestion(question.getId().value()) > 0L) {
      throw new QuestionInUseException("Question is still referenced by one or more quezaps");
    }

    remove(question);

    return new Output.QuestionDeleted();
  }

  private void remove(Question question) {
    question.delete();
    questionRepository.delete(question);
  }
}

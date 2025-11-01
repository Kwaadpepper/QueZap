package com.quezap.application.usecases.questions;

import com.quezap.application.ports.questions.RemoveQuestion.Input;
import com.quezap.application.ports.questions.RemoveQuestion.RemoveQuestionUsecase;
import com.quezap.domain.errors.questions.DeleteQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveQuestionTest {
  private final QuestionRepository questionRepository;
  private final RemoveQuestionUsecase usecase;

  public RemoveQuestionTest() {
    this.questionRepository = MockEntity.mock(QuestionRepository.class);
    this.usecase = new RemoveQuestionHandler(questionRepository);
  }

  @Test
  void canDeleteQuestion() {
    // GIVEN
    var questionId = QuestionId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input(questionId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    final var question = MockEntity.mock(Question.class);
    Mockito.when(questionRepository.find(questionId)).thenReturn(MockEntity.optional(question));
    usecase.handle(input, unitOfWork);

    // THEN
    Mockito.verify(question).delete();
    Mockito.verify(questionRepository).delete(question);
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void cannotDeleteNonExistingQuestion() {
    // GIVEN
    var questionId = QuestionId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input(questionId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(questionRepository.find(questionId)).thenReturn(MockEntity.optional());

    // THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> usecase.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(DeleteQuestionError.QUESTION_NOT_FOUND.getCode());
  }
}

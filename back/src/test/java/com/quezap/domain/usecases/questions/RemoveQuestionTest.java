package com.quezap.domain.usecases.questions;

import java.util.UUID;

import com.quezap.domain.errors.questions.DeleteQuestionError;
import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveQuestionTest {
  private final QuestionRepository questionRepository;
  private final RemoveQuestion.Handler deleteQuestionHandler;

  public RemoveQuestionTest() {
    this.questionRepository = Mockito.mock(QuestionRepository.class);
    this.deleteQuestionHandler = new RemoveQuestion.Handler(questionRepository);
  }

  @Test
  void canDeleteQuestion() {
    // GIVEN
    var questionId = new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new RemoveQuestion.Input(questionId);

    // WHEN
    final var question = Mockito.mock(Question.class);
    Mockito.when(questionRepository.find(questionId)).thenReturn(question);
    deleteQuestionHandler.handle(input);

    // THEN
    Mockito.verify(question).delete();
    Mockito.verify(questionRepository).delete(question);
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void cannotDeleteNonExistingQuestion() {
    // GIVEN
    var questionId = new QuestionId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new RemoveQuestion.Input(questionId);

    // WHEN
    Mockito.when(questionRepository.find(questionId)).thenReturn(null);

    // THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> deleteQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(DeleteQuestionError.QUESTION_NOT_FOUND.getCode());
  }
}

package com.quezap.domain.usecases.questions;

import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.ddd.usecases.TransactionRegistrar;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AddQuestionTest {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager pictureManager;
  private final TransactionRegistrar transactionRegistrar;
  private final AddQuestion.Handler addQuestionHandler;

  public AddQuestionTest() {
    this.questionRepository = Mockito.mock(QuestionRepository.class);
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.pictureManager = Mockito.mock(QuestionPictureManager.class);
    this.transactionRegistrar = Mockito.mock(TransactionRegistrar.class);
    this.addQuestionHandler =
        new AddQuestion.Handler(
            questionRepository, themeRepository, pictureManager, transactionRegistrar);
  }

  @Test
  void canAddQuestion() {
    // GIVEN
    // var type = QuestionType.BOOLEAN;
    // var value = "Will this test be working?";
    // Picture picture = null;
    // var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    // Set<Answer> answers = Set.of(Mockito.mock(Answer.class));
    // var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN

    // THEN
  }
}

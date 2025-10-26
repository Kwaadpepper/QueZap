package com.quezap.domain.usecases.questions;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.errors.questions.AddQuestionError;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.TransactionRegistrar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddQuizzQuestionTest {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager pictureManager;
  private final TransactionRegistrar transactionRegistrar;
  private final AddQuestion.Handler addQuestionHandler;

  public AddQuizzQuestionTest() {
    this.questionRepository = Mockito.mock(QuestionRepository.class);
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.pictureManager = Mockito.mock(QuestionPictureManager.class);
    this.transactionRegistrar = Mockito.mock(TransactionRegistrar.class);
    this.addQuestionHandler =
        new AddQuestion.Handler(
            questionRepository, themeRepository, pictureManager, transactionRegistrar);
  }

  @Test
  void canAddQuizzQuestionWithThreeAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithFourAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false),
            new AddQuestion.Input.AnswerData("D the D response", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithPicture() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG);
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));
    Mockito.when(pictureManager.store(picture)).thenReturn(Mockito.mock(Picture.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Mockito.verify(pictureManager).store(picture);
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithPicturesOnAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG);
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData(
                "Yes",
                new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG),
                true),
            new AddQuestion.Input.AnswerData(
                "No",
                new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG),
                false),
            new AddQuestion.Input.AnswerData(
                "After waiting 3 minutes",
                new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG),
                false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));
    Mockito.when(pictureManager.store(picture)).thenReturn(Mockito.mock(Picture.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Mockito.verify(pictureManager, Mockito.times(4)).store(Mockito.any(PictureUploadData.class));
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void cannotAddQuizzQuestionWhenThemeDoesNotExists() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false),
            new AddQuestion.Input.AnswerData("D the D response", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(null);

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.THEME_DOES_NOT_EXISTS.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithNoCorrectAnswer() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, false),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithLessThanThreeAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithMoreThanFourAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.of(
            new AddQuestion.Input.AnswerData("Yes", null, true),
            new AddQuestion.Input.AnswerData("No", null, false),
            new AddQuestion.Input.AnswerData("Maybe", null, false),
            new AddQuestion.Input.AnswerData("After waiting 3 minutes", null, false),
            new AddQuestion.Input.AnswerData("D the D response", null, false));
    var theme = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new AddQuestion.Input.Quizz(value, answers, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }
}

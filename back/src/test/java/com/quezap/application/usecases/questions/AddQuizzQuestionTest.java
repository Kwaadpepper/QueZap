package com.quezap.application.usecases.questions;

import java.io.InputStream;
import java.util.Set;

import com.quezap.application.usecases.questions.AddQuestion.Input;
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
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddQuizzQuestionTest {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager pictureManager;
  private final AddQuestion.Handler addQuestionHandler;

  public AddQuizzQuestionTest() {
    this.questionRepository = MockEntity.mock(QuestionRepository.class);
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.pictureManager = MockEntity.mock(QuestionPictureManager.class);
    this.addQuestionHandler =
        new AddQuestion.Handler(questionRepository, themeRepository, pictureManager);
  }

  @Test
  void canAddQuizzQuestionWithThreeAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN
    addQuestionHandler.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithFourAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false),
            new Input.AnswerData("D the D response", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN
    addQuestionHandler.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithPicture() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG);
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(pictureManager.store(picture)).thenReturn(MockEntity.mock(Picture.class));

    // WHEN
    addQuestionHandler.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
    Mockito.verify(pictureManager).store(picture);
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddQuizzQuestionWithPicturesOnAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG);
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData(
                "Yes",
                new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG),
                true),
            new Input.AnswerData(
                "No",
                new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG),
                false),
            new Input.AnswerData(
                "After waiting 3 minutes",
                new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG),
                false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(pictureManager.store(picture)).thenReturn(MockEntity.mock(Picture.class));

    // WHEN
    addQuestionHandler.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
    Mockito.verify(pictureManager, Mockito.times(4)).store(MockEntity.any(PictureUploadData.class));
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void cannotAddQuizzQuestionWhenThemeDoesNotExists() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false),
            new Input.AnswerData("D the D response", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional());

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.THEME_DOES_NOT_EXISTS.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithNoCorrectAnswer() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, false),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithLessThanThreeAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true), new Input.AnswerData("No", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }

  @Test
  void cannotAddQuizzQuestionWithMoreThanFourAnswers() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var answers =
        Set.<Input.AnswerData>of(
            new Input.AnswerData("Yes", null, true),
            new Input.AnswerData("No", null, false),
            new Input.AnswerData("Maybe", null, false),
            new Input.AnswerData("After waiting 3 minutes", null, false),
            new Input.AnswerData("D the D response", null, false));
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new Input.Quizz(value, answers, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.INVALID_QUESTION_DATA.getCode());
  }
}

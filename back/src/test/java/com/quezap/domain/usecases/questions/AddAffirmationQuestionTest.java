package com.quezap.domain.usecases.questions;

import java.io.InputStream;

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

class AddAffirmationQuestionTest {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager pictureManager;
  private final TransactionRegistrar transactionRegistrar;
  private final AddQuestion.Handler addQuestionHandler;

  public AddAffirmationQuestionTest() {
    this.questionRepository = Mockito.mock(QuestionRepository.class);
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.pictureManager = Mockito.mock(QuestionPictureManager.class);
    this.transactionRegistrar = Mockito.mock(TransactionRegistrar.class);
    this.addQuestionHandler =
        new AddQuestion.Handler(
            questionRepository, themeRepository, pictureManager, transactionRegistrar);
  }

  @Test
  void canAddAffirmationQuestion() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var isTrue = true;
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddAffirmationQuestionWithNegativeAnswer() {
    // GIVEN
    var value = "Will ths test fail?";
    PictureUploadData picture = null;
    var isTrue = false;
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(Mockito.mock(Theme.class));

    // WHEN
    addQuestionHandler.handle(input);

    // THEN
    Mockito.verify(questionRepository).save(Mockito.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddAffirmationQuestionWithPicture() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(Mockito.mock(InputStream.class), 10L, PictureType.PNG);
    var isTrue = true;
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, theme);

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
  void cannotAddQuestionToNonExistingTheme() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var isTrue = true;
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, theme);

    Mockito.when(themeRepository.find(theme)).thenReturn(null);

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> addQuestionHandler.handle(input))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.THEME_DOES_NOT_EXISTS.getCode());
  }
}

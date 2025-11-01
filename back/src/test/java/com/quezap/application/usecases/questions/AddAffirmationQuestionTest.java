package com.quezap.application.usecases.questions;

import java.io.InputStream;

import com.quezap.application.ports.questions.AddQuestion;
import com.quezap.application.ports.questions.AddQuestion.AddQuestionUsecase;
import com.quezap.domain.errors.questions.AddQuestionError;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.models.valueobjects.pictures.PictureUploadData;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddAffirmationQuestionTest {
  private final QuestionRepository questionRepository;
  private final ThemeRepository themeRepository;
  private final QuestionPictureManager pictureManager;
  private final AddQuestionUsecase usecase;

  public AddAffirmationQuestionTest() {
    this.questionRepository = MockEntity.mock(QuestionRepository.class);
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.pictureManager = MockEntity.mock(QuestionPictureManager.class);
    this.usecase = new AddQuestionHandler(questionRepository, themeRepository, pictureManager);
  }

  @Test
  void canAddAffirmationQuestion() {
    // GIVEN
    var value = "Will this test be working?";
    PictureUploadData picture = null;
    var isTrue = true;
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, themeId);
    var unitEvts = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN
    usecase.handle(input, unitEvts);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
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
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN
    usecase.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canAddAffirmationQuestionWithPicture() {
    // GIVEN
    var value = "Will this test be working?";
    var picture = new PictureUploadData(MockEntity.mock(InputStream.class), 10L, PictureType.PNG);
    var isTrue = true;
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var input = new AddQuestion.Input.Affirmation(value, isTrue, picture, theme);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(pictureManager.store(picture)).thenReturn(MockEntity.mock(Picture.class));

    // WHEN
    usecase.handle(input, unitOfWork);

    // THEN
    Mockito.verify(questionRepository).persist(MockEntity.any());
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
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(theme)).thenReturn(MockEntity.optional());

    // WHEN / THEN
    Assertions.assertThatExceptionOfType(DomainConstraintException.class)
        .isThrownBy(() -> usecase.handle(input, unitOfWork))
        .extracting(DomainConstraintException::getCode)
        .isEqualTo(AddQuestionError.THEME_DOES_NOT_EXISTS.getCode());
  }
}

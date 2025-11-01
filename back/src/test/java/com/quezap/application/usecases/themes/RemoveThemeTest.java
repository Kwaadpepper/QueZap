package com.quezap.application.usecases.themes;

import java.util.Set;

import com.quezap.application.ports.themes.RemoveTheme.Input;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUsecase;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveThemeTest {
  private final ThemeRepository themeRepository;
  private final QuestionRepository questionRepository;
  private final RemoveThemeUsecase usecase;

  public RemoveThemeTest() {
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.questionRepository = MockEntity.mock(QuestionRepository.class);
    this.usecase = new RemoveThemeHandler(themeRepository, questionRepository);
  }

  @Test
  void canRemoveTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(questionRepository.countWithThemes(MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(0L);

    // WHEN
    usecase.handle(removeThemeInput, unitOfWork);

    // THEN
    Mockito.verify(themeRepository).delete(MockEntity.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotRemoveNonExistingTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional());
    Mockito.when(questionRepository.countWithThemes(MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(0L);

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class, () -> usecase.handle(removeThemeInput, unitOfWork));
  }

  @Test
  void cannotRemoveThemeWithQuestions() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));

    Mockito.when(questionRepository.countWithThemes(MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(1L);

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class, () -> usecase.handle(removeThemeInput, unitOfWork));
  }
}

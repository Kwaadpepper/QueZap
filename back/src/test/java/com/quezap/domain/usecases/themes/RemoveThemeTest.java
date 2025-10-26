package com.quezap.domain.usecases.themes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveThemeTest {
  private final ThemeRepository themeRepository;
  private final QuestionRepository questionRepository;
  private final RemoveTheme.Handler removeThemeHandler;

  public RemoveThemeTest() {
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.questionRepository = Mockito.mock(QuestionRepository.class);
    this.removeThemeHandler = new RemoveTheme.Handler(themeRepository, questionRepository);
  }

  @Test
  void canRemoveTheme() {
    // GIVEN
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var removeThemeInput = new RemoveTheme.Input(themeId);

    Mockito.when(themeRepository.find(themeId)).thenReturn(Mockito.mock(Theme.class));
    Mockito.when(
            questionRepository.paginateWithThemes(
                Mockito.any(Pagination.class), Mockito.eq(Set.of(themeId))))
        .thenReturn(PageOf.empty(Mockito.any(Pagination.class)));

    // WHEN
    removeThemeHandler.handle(removeThemeInput);

    // THEN
    Mockito.verify(themeRepository).delete(Mockito.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotRemoveNonExistingTheme() {
    // GIVEN
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var removeThemeInput = new RemoveTheme.Input(themeId);

    Mockito.when(themeRepository.find(themeId)).thenReturn(null);

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class, () -> removeThemeHandler.handle(removeThemeInput));
  }

  @Test
  void cannotRemoveThemeWithQuestions() {
    // GIVEN
    var themeId = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var removeThemeInput = new RemoveTheme.Input(themeId);

    Mockito.when(themeRepository.find(themeId)).thenReturn(Mockito.mock(Theme.class));

    var mockedQuestion = Mockito.mock(Question.class);
    Mockito.when(
            questionRepository.paginateWithThemes(
                Mockito.any(Pagination.class), Mockito.eq(Set.of(themeId))))
        .thenReturn(PageOf.of(Pagination.ofIndexes(0L, 1L), List.of(mockedQuestion), 1L));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class, () -> removeThemeHandler.handle(removeThemeInput));
  }
}

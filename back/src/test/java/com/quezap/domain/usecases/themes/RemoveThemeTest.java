package com.quezap.domain.usecases.themes;

import java.util.List;
import java.util.Set;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoveThemeTest {
  private final ThemeRepository themeRepository;
  private final QuestionRepository questionRepository;
  private final RemoveTheme.Handler removeThemeHandler;

  public RemoveThemeTest() {
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.questionRepository = MockEntity.mock(QuestionRepository.class);
    this.removeThemeHandler = new RemoveTheme.Handler(themeRepository, questionRepository);
  }

  @Test
  void canRemoveTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new RemoveTheme.Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(
            questionRepository.paginateWithThemes(
                MockEntity.any(Pagination.class), MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(PageOf.empty(MockEntity.mock(Pagination.class)));

    // WHEN
    removeThemeHandler.handle(removeThemeInput, unitOfWork);

    // THEN
    Mockito.verify(themeRepository).delete(MockEntity.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotRemoveNonExistingTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new RemoveTheme.Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional());
    Mockito.when(
            questionRepository.paginateWithThemes(
                MockEntity.any(Pagination.class), MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(PageOf.empty(MockEntity.mock(Pagination.class)));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> removeThemeHandler.handle(removeThemeInput, unitOfWork));
  }

  @Test
  void cannotRemoveThemeWithQuestions() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var removeThemeInput = new RemoveTheme.Input(themeId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));

    var mockedQuestion = MockEntity.mock(Question.class);
    Mockito.when(
            questionRepository.paginateWithThemes(
                MockEntity.any(Pagination.class), MockEntity.eq(Set.<ThemeId>of(themeId))))
        .thenReturn(PageOf.of(Pagination.ofIndexes(0L, 1L), List.of(mockedQuestion), 1L));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> removeThemeHandler.handle(removeThemeInput, unitOfWork));
  }
}

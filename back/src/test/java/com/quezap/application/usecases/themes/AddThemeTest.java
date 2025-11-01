package com.quezap.application.usecases.themes;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddThemeTest {
  private final ThemeRepository themeRepository;
  private final AddTheme.Handler addThemeHandler;

  public AddThemeTest() {
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.addThemeHandler = new AddTheme.Handler(themeRepository);
  }

  @Test
  void canAddTheme() {
    // GIVEN
    var themeName = new ThemeName("New Theme");
    var addThemeInput = new AddTheme.Input(themeName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    addThemeHandler.handle(addThemeInput, unitOfWork);

    // THEN
    Mockito.verify(themeRepository).persist(MockEntity.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotAddDuplicateTheme() {
    // GIVEN
    var themeName = new ThemeName("Existing Theme");
    var addThemeInput = new AddTheme.Input(themeName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.findByName(themeName))
        .thenReturn(MockEntity.optional(Theme.class));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          addThemeHandler.handle(addThemeInput, unitOfWork);
        });
  }
}

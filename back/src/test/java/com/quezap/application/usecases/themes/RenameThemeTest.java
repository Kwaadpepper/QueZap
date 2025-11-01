package com.quezap.application.usecases.themes;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RenameThemeTest {
  private final ThemeRepository themeRepository;
  private final RenameTheme.Handler renameThemeHandler;

  public RenameThemeTest() {
    this.themeRepository = MockEntity.mock(ThemeRepository.class);
    this.renameThemeHandler = new RenameTheme.Handler(themeRepository);
  }

  @Test
  void canRenameTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("New Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(themeRepository.findByName(newName)).thenReturn(MockEntity.optional());

    // WHEN
    renameThemeHandler.handle(renameThemeInput, unitOfWork);

    // THEN
    Mockito.verify(themeRepository).persist(MockEntity.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotRenameNonExistingTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("New Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional());

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          renameThemeHandler.handle(renameThemeInput, unitOfWork);
        });
  }

  @Test
  void cannotRenameThemeToExistingName() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("Existing Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    Mockito.when(themeRepository.find(themeId)).thenReturn(MockEntity.optional(Theme.class));
    Mockito.when(themeRepository.findByName(newName)).thenReturn(MockEntity.optional(Theme.class));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          renameThemeHandler.handle(renameThemeInput, unitOfWork);
        });
  }
}

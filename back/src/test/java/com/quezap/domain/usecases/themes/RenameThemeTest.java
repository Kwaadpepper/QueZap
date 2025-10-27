package com.quezap.domain.usecases.themes;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RenameThemeTest {
  private final ThemeRepository themeRepository;
  private final RenameTheme.Handler renameThemeHandler;

  public RenameThemeTest() {
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.renameThemeHandler = new RenameTheme.Handler(themeRepository);
  }

  @Test
  void canRenameTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("New Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);

    Mockito.when(themeRepository.find(themeId)).thenReturn(Mockito.mock(Theme.class));
    Mockito.when(themeRepository.findByName(newName)).thenReturn(null);

    // WHEN
    renameThemeHandler.handle(renameThemeInput);

    // THEN
    Mockito.verify(themeRepository).save(Mockito.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotRenameNonExistingTheme() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("New Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);

    Mockito.when(themeRepository.find(themeId)).thenReturn(null);

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          renameThemeHandler.handle(renameThemeInput);
        });
  }

  @Test
  void cannotRenameThemeToExistingName() {
    // GIVEN
    var themeId = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var newName = new ThemeName("Existing Theme Name");
    var renameThemeInput = new RenameTheme.Input(themeId, newName);

    Mockito.when(themeRepository.find(themeId)).thenReturn(Mockito.mock(Theme.class));
    Mockito.when(themeRepository.findByName(newName)).thenReturn(Mockito.mock(Theme.class));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          renameThemeHandler.handle(renameThemeInput);
        });
  }
}

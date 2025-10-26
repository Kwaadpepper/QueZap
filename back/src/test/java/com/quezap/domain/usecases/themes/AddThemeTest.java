package com.quezap.domain.usecases.themes;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddThemeTest {
  private final ThemeRepository themeRepository;
  private final AddTheme.Handler addThemeHandler;

  public AddThemeTest() {
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.addThemeHandler = new AddTheme.Handler(themeRepository);
  }

  @Test
  void canAddTheme() {
    // GIVEN
    var themeName = new ThemeName("New Theme");
    var addThemeInput = new AddTheme.Input(themeName);

    // WHEN
    addThemeHandler.handle(addThemeInput);

    // THEN
    Mockito.verify(themeRepository).save(Mockito.any(Theme.class));
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotAddDuplicateTheme() {
    // GIVEN
    var themeName = new ThemeName("Existing Theme");
    var addThemeInput = new AddTheme.Input(themeName);

    Mockito.when(themeRepository.findByName(themeName)).thenReturn(Mockito.mock(Theme.class));

    // WHEN / THEN
    Assertions.assertThrows(
        DomainConstraintException.class,
        () -> {
          addThemeHandler.handle(addThemeInput);
        });
  }
}

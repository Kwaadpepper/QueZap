package com.quezap.domain.usecases.themes;

import com.quezap.domain.errors.themes.AddThemeError;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface AddTheme {

  record Input(ThemeName name) implements UseCaseInput {}

  record Output(ThemeId id) implements UseCaseOutput {}

  final class Handler implements UseCaseHandler<Input, Output>, AddTheme {
    private final ThemeRepository themeRepository;

    public Handler(ThemeRepository themeRepository) {
      this.themeRepository = themeRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var themeName = usecaseInput.name();

      if (themeRepository.findByName(themeName) != null) {
        throw new DomainConstraintException(AddThemeError.THEME_ALREADY_EXISTS);
      }

      final var theme = new Theme(themeName);

      themeRepository.save(theme);

      return new Output(theme.getId());
    }
  }
}

package com.quezap.application.usecases.themes;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.themes.AddThemeError;
import com.quezap.application.ports.themes.AddTheme.AddThemeUsecase;
import com.quezap.application.ports.themes.AddTheme.Input;
import com.quezap.application.ports.themes.AddTheme.Output;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class AddThemeHandler implements AddThemeUsecase {
  private final ThemeRepository themeRepository;

  public AddThemeHandler(ThemeRepository themeRepository) {
    this.themeRepository = themeRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var themeName = usecaseInput.name();

    if (themeRepository.findByName(themeName).isPresent()) {
      throw new ApplicationConstraintException(AddThemeError.THEME_ALREADY_EXISTS);
    }

    final var theme = new Theme(themeName);

    themeRepository.persist(theme);

    return new Output(theme.getId());
  }
}

package com.quezap.application.usecases.themes;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.themes.RenameTheme.Input;
import com.quezap.application.ports.themes.RenameTheme.Output;
import com.quezap.application.ports.themes.RenameTheme.RenameThemeUsecase;
import com.quezap.domain.errors.themes.RenameThemeError;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class RenameThemeHandler implements RenameThemeUsecase {
  private final ThemeRepository themeRepository;

  public RenameThemeHandler(ThemeRepository themeRepository) {
    this.themeRepository = themeRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var themeId = usecaseInput.id();
    final var newName = usecaseInput.newName();

    if (themeRepository.findByName(newName).isPresent()) {
      throw new DomainConstraintException(RenameThemeError.THEME_NAME_ALREADY_EXISTS);
    }

    themeRepository
        .find(themeId)
        .ifPresentOrElse(
            theme -> renameAndPersist(theme, newName),
            DomainConstraintException.throwWith(RenameThemeError.THEME_DOES_NOT_EXISTS));

    return new Output.ThemeRenamed();
  }

  private void renameAndPersist(Theme theme, ThemeName newName) {
    theme.setName(newName);
    themeRepository.persist(theme);
  }
}

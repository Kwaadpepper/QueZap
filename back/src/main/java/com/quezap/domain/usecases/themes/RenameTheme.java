package com.quezap.domain.usecases.themes;

import com.quezap.domain.errors.themes.RenameThemeError;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface RenameTheme {
  record Input(ThemeId id, ThemeName newName) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record ThemeRemoved() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, RenameTheme {
    private final ThemeRepository themeRepository;

    public Handler(ThemeRepository themeRepository) {
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

      return new Output.ThemeRemoved();
    }

    private void renameAndPersist(Theme theme, ThemeName newName) {
      theme.setName(newName);
      themeRepository.save(theme);
    }
  }
}

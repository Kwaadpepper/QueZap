package com.quezap.application.usecases.themes;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.themes.RenameThemeError;
import com.quezap.application.ports.themes.RenameTheme.Input;
import com.quezap.application.ports.themes.RenameTheme.Output;
import com.quezap.application.ports.themes.RenameTheme.RenameThemeUsecase;
import com.quezap.application.ports.themes.RenameTheme.ThemeForbiddenException;
import com.quezap.application.ports.themes.RenameTheme.ThemeInUseException;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

import org.jspecify.annotations.Nullable;

@Usecase
final class RenameThemeHandler implements RenameThemeUsecase {
  private final ThemeRepository themeRepository;
  private final @Nullable QuestionRepository questionRepository;

  public RenameThemeHandler(ThemeRepository themeRepository) {
    this(themeRepository, null);
  }

  @Autowired
  public RenameThemeHandler(
      ThemeRepository themeRepository, @Nullable QuestionRepository questionRepository) {
    this.themeRepository = themeRepository;
    this.questionRepository = questionRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var themeId = usecaseInput.id();
    final var newName = usecaseInput.newName();

    final var theme =
        themeRepository
            .find(themeId)
            .orElseThrow(
                () -> new ApplicationConstraintException(RenameThemeError.THEME_DOES_NOT_EXISTS));

    if (!usecaseInput.isAdmin()) {
      final var requesterId = usecaseInput.requesterId();
      final var createdBy = theme.getCreatedBy();
      if (requesterId == null || createdBy == null || !createdBy.equals(requesterId)) {
        throw new ThemeForbiddenException("You are not allowed to rename this theme");
      }
      if (questionRepository != null && questionRepository.countWithThemes(Set.of(themeId)) > 0L) {
        throw new ThemeInUseException("Cannot rename a theme that is used by existing questions");
      }
    }

    if (themeRepository.findByName(newName).isPresent()) {
      throw new ApplicationConstraintException(RenameThemeError.THEME_NAME_ALREADY_EXISTS);
    }

    renameAndPersist(theme, newName);

    return new Output.ThemeRenamed();
  }

  private void renameAndPersist(Theme theme, ThemeName newName) {
    theme.setName(newName);
    themeRepository.persist(theme);
  }
}

package com.quezap.application.usecases.themes;

import java.util.Set;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.themes.RemoveTheme.Input;
import com.quezap.application.ports.themes.RemoveTheme.Output;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUseCase;
import com.quezap.domain.errors.themes.RemoveThemeError;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class RemoveThemeHandler implements RemoveThemeUseCase {
  private final ThemeRepository themeRepository;
  private final QuestionRepository questionRepository;

  public RemoveThemeHandler(
      ThemeRepository themeRepository, QuestionRepository questionRepository) {
    this.themeRepository = themeRepository;
    this.questionRepository = questionRepository;
  }

  @Override
  public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
    final var themeId = usecaseInput.id();
    final var themeSet = Set.<ThemeId>of(themeId);

    if (questionRepository.countWithThemes(themeSet) > 0L) {
      throw new DomainConstraintException(RemoveThemeError.THEME_HAS_QUESTIONS);
    }

    themeRepository
        .find(themeId)
        .ifPresentOrElse(
            themeRepository::delete,
            DomainConstraintException.throwWith(RemoveThemeError.THEME_DOES_NOT_EXISTS));

    return new Output.ThemeRemoved();
  }
}

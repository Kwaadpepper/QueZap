package com.quezap.domain.usecases.themes;

import java.util.Set;

import com.quezap.domain.errors.themes.RemoveThemeError;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.pagination.Pagination;

public sealed interface RemoveTheme {
  record Input(ThemeId id) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record ThemeRemoved() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, RemoveTheme {
    private final ThemeRepository themeRepository;
    private final QuestionRepository questionRepository;

    public Handler(ThemeRepository themeRepository, QuestionRepository questionRepository) {
      this.themeRepository = themeRepository;
      this.questionRepository = questionRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var themeId = usecaseInput.id();
      final var theme = themeRepository.find(themeId.value());
      final var pagination = Pagination.firstPage();
      final var themeSet = Set.of(themeId);

      if (theme != null) {
        throw new DomainConstraintException(RemoveThemeError.THEME_DOES_NOT_EXISTS);
      }

      if (questionRepository.paginateWithThemes(pagination, themeSet).totalItems() > 0) {
        throw new DomainConstraintException(RemoveThemeError.THEME_HAS_QUESTIONS);
      }

      themeRepository.delete(theme);

      return new Output.ThemeRemoved();
    }
  }
}

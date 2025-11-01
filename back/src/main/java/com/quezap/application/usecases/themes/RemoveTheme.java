package com.quezap.application.usecases.themes;

import java.util.Set;

import com.quezap.domain.errors.themes.RemoveThemeError;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

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
}

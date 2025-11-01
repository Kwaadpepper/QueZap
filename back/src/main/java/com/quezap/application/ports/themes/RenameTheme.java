package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface RenameTheme {

  record Input(ThemeId id, ThemeName newName) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record ThemeRenamed() implements Output {}
  }

  public interface RenameThemeUseCase
      extends UseCaseHandler<RenameTheme.Input, RenameTheme.Output> {}
}

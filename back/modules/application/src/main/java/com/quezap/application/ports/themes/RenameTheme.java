package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RenameTheme {

  record Input(ThemeId id, ThemeName newName) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record ThemeRenamed() implements Output {}
  }

  public interface RenameThemeUsecase
      extends UsecaseHandler<RenameTheme.Input, RenameTheme.Output> {}
}

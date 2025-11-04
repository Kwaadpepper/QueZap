package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RemoveTheme {

  record Input(ThemeId id) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record ThemeRemoved() implements Output {}
  }

  public interface RemoveThemeUsecase
      extends UsecaseHandler<RemoveTheme.Input, RemoveTheme.Output> {}
}

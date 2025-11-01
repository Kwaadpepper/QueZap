package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface RemoveTheme {

  record Input(ThemeId id) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record ThemeRemoved() implements Output {}
  }

  public interface RemoveThemeUseCase
      extends UseCaseHandler<RemoveTheme.Input, RemoveTheme.Output> {}
}

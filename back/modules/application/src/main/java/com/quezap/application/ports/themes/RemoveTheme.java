package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface RemoveTheme {

  record Input(ThemeId id, @Nullable UserId requesterId, boolean isAdmin) implements UsecaseInput {
    public Input(ThemeId id) {
      this(id, null, false);
    }
  }

  sealed interface Output extends UsecaseOutput {
    record ThemeRemoved() implements Output {}
  }

  class ThemeForbiddenException extends RuntimeException {
    public ThemeForbiddenException(String message) {
      super(message);
    }
  }

  public interface RemoveThemeUsecase
      extends UsecaseHandler<RemoveTheme.Input, RemoveTheme.Output> {}
}

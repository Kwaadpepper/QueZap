package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface RenameTheme {

  record Input(ThemeId id, ThemeName newName, @Nullable UserId requesterId, boolean isAdmin)
      implements UsecaseInput {
    public Input(ThemeId id, ThemeName newName) {
      this(id, newName, null, false);
    }
  }

  sealed interface Output extends UsecaseOutput {
    record ThemeRenamed() implements Output {}
  }

  class ThemeForbiddenException extends RuntimeException {
    public ThemeForbiddenException(String message) {
      super(message);
    }
  }

  class ThemeInUseException extends RuntimeException {
    public ThemeInUseException(String message) {
      super(message);
    }
  }

  public interface RenameThemeUsecase
      extends UsecaseHandler<RenameTheme.Input, RenameTheme.Output> {}
}

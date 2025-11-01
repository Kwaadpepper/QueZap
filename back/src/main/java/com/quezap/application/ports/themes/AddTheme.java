package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AddTheme {

  record Input(ThemeName name) implements UsecaseInput {}

  record Output(ThemeId id) implements UsecaseOutput {}

  public interface AddThemeUsecase extends UsecaseHandler<AddTheme.Input, AddTheme.Output> {}
}

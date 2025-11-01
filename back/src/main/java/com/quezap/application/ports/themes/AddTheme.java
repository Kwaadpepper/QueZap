package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface AddTheme {

  record Input(ThemeName name) implements UseCaseInput {}

  record Output(ThemeId id) implements UseCaseOutput {}

  public interface AddThemeUseCase extends UseCaseHandler<AddTheme.Input, AddTheme.Output> {}
}

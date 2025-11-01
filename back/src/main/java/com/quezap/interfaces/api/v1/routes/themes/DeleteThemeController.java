package com.quezap.interfaces.api.v1.routes.themes;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.ports.themes.RemoveTheme;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUseCase;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@RestController
public class DeleteThemeController {
  private final UseCaseExecutor executor;
  private final RemoveThemeUseCase usecase;

  DeleteThemeController(UseCaseExecutor executor, RemoveThemeUseCase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @DeleteMapping("apiv1/themes/{id}")
  void delete(@PathVariable("id") ThemeId id) {
    final var input = new RemoveTheme.Input(id);

    executor.execute(usecase, input);
  }
}

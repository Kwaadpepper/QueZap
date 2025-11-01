package com.quezap.interfaces.api.v1.routes.themes;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.usecases.themes.RemoveTheme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@RestController
public class DeleteThemeController {
  private final UseCaseExecutor executor;
  private final RemoveTheme.Handler handler;

  DeleteThemeController(UseCaseExecutor executor, RemoveTheme.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @DeleteMapping("apiv1/themes/{id}")
  void delete(@PathVariable("id") ThemeId id) {
    final var input = new RemoveTheme.Input(id);

    executor.execute(handler, input);
  }
}

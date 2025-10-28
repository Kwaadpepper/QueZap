package com.quezap.application.api.v1.routes.themes;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.request.themes.RenameThemeRequest;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.usecases.themes.RenameTheme;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import jakarta.validation.Valid;

@RestController
public class RenameThemeController {
  private final UseCaseExecutor executor;
  private final RenameTheme.Handler handler;

  RenameThemeController(UseCaseExecutor executor, RenameTheme.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @PatchMapping("apiv1/themes/rename/{id}")
  void rename(@PathVariable("id") ThemeId id, @RequestBody @Valid RenameThemeRequest request) {
    final var name = request.name();
    final var input = new RenameTheme.Input(id, name);

    executor.execute(handler, input);
  }
}

package com.quezap.application.api.v1.routes.themes;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.usecases.themes.RemoveTheme;

@RestController
public class DeleteThemeController {
  private final RemoveTheme.Handler handler;

  DeleteThemeController(RemoveTheme.Handler handler) {
    this.handler = handler;
  }

  @DeleteMapping("apiv1/themes/{id}")
  void delete(@PathVariable("id") ThemeId id) {
    final var input = new RemoveTheme.Input(id);

    handler.handle(input);
  }
}

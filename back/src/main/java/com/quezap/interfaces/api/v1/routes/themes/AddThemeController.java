package com.quezap.interfaces.api.v1.routes.themes;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.usecases.themes.AddTheme;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.interfaces.api.v1.dto.request.themes.CreateThemeRequest;
import com.quezap.interfaces.api.v1.dto.response.themes.ThemeIdDto;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import jakarta.validation.Valid;

@RestController
public class AddThemeController {
  private final UseCaseExecutor executor;
  private final AddTheme.Handler handler;

  AddThemeController(UseCaseExecutor executor, AddTheme.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @PostMapping("apiv1/themes")
  ThemeIdDto add(@RequestBody @Valid CreateThemeRequest request) {
    final var themName = request.name();
    final var input = new AddTheme.Input(themName);

    final var output = executor.execute(handler, input);

    return toDto(output.id());
  }

  private ThemeIdDto toDto(ThemeId id) {
    return new ThemeIdDto(id.value());
  }
}

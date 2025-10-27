package com.quezap.application.api.v1.routes.themes;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.request.themes.CreateThemeRequest;
import com.quezap.application.api.v1.dto.response.themes.ThemeIdDto;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.usecases.themes.AddTheme;

import jakarta.validation.Valid;

@RestController
public class AddThemeController {
  private final AddTheme.Handler handler;

  AddThemeController(AddTheme.Handler handler) {
    this.handler = handler;
  }

  @PostMapping("apiv1/themes")
  ThemeIdDto add(@RequestBody @Valid CreateThemeRequest request) {
    final var themName = request.name();
    final var input = new AddTheme.Input(themName);

    final var output = handler.handle(input);

    return toDto(output.id());
  }

  private ThemeIdDto toDto(ThemeId id) {
    return new ThemeIdDto(id.value());
  }
}

package com.quezap.api.v1.routes.themes;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.api.v1.dto.request.themes.CreateThemeRequest;
import com.quezap.api.v1.dto.response.themes.ThemeIdDto;
import com.quezap.application.ports.themes.AddTheme;
import com.quezap.application.ports.themes.AddTheme.AddThemeUsecase;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;

@RestController
public class AddThemeController {
  private final UsecaseExecutor executor;
  private final AddThemeUsecase usecase;

  AddThemeController(UsecaseExecutor executor, AddThemeUsecase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @PostMapping("apiv1/themes")
  ThemeIdDto add(@RequestBody @Valid CreateThemeRequest request) {
    final var themName = request.name();
    final var input = new AddTheme.Input(themName);

    final var output = executor.execute(usecase, input);

    return toDto(output.id());
  }

  private ThemeIdDto toDto(ThemeId id) {
    return new ThemeIdDto(id.value());
  }
}

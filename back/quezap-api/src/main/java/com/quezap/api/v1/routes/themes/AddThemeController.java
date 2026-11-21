package com.quezap.api.v1.routes.themes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.api.v1.dto.request.themes.CreateThemeRequest;
import com.quezap.api.v1.dto.response.themes.ThemeIdDto;
import com.quezap.application.ports.themes.AddTheme;
import com.quezap.application.ports.themes.AddTheme.AddThemeUsecase;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
public class AddThemeController {
  private final UsecaseExecutor executor;
  private final AddThemeUsecase usecase;
  private final @Nullable UserRepository userRepository;

  AddThemeController(UsecaseExecutor executor, AddThemeUsecase usecase) {
    this(executor, usecase, null);
  }

  @Autowired
  AddThemeController(
      UsecaseExecutor executor, AddThemeUsecase usecase, @Nullable UserRepository userRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.userRepository = userRepository;
  }

  @PostMapping("apiv1/themes")
  ThemeIdDto add(@RequestBody @Valid CreateThemeRequest request) {
    final var auth = ThemeAuthHelper.requireAuthentication(null);
    final var createdBy = ThemeAuthHelper.requireUserId(auth, userRepository);
    final var themeName = request.name();
    final var input = new AddTheme.Input(themeName, createdBy);

    final var output = executor.execute(usecase, input);

    return toDto(output.id());
  }

  private ThemeIdDto toDto(ThemeId id) {
    return new ThemeIdDto(id.value());
  }
}

package com.quezap.api.v1.routes.themes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.ports.themes.RemoveTheme;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUsecase;
import com.quezap.application.ports.themes.RemoveTheme.ThemeForbiddenException;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import org.jspecify.annotations.Nullable;

@RestController
public class DeleteThemeController {
  private final UsecaseExecutor executor;
  private final RemoveThemeUsecase usecase;
  private final @Nullable UserRepository userRepository;

  DeleteThemeController(UsecaseExecutor executor, RemoveThemeUsecase usecase) {
    this(executor, usecase, null);
  }

  @Autowired
  DeleteThemeController(
      UsecaseExecutor executor,
      RemoveThemeUsecase usecase,
      @Nullable UserRepository userRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.userRepository = userRepository;
  }

  @DeleteMapping("apiv1/themes/{id}")
  void delete(@PathVariable("id") ThemeId id, @Nullable Authentication authentication) {
    final var effectiveAuth = ThemeAuthHelper.requireAuthentication(authentication);
    final var requesterId = ThemeAuthHelper.requireUserId(effectiveAuth, userRepository);
    final var isAdmin = ThemeAuthHelper.isAdmin(effectiveAuth);

    final var input = new RemoveTheme.Input(id, requesterId, isAdmin);

    try {
      executor.execute(usecase, input);
    } catch (ThemeForbiddenException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    }
  }
}

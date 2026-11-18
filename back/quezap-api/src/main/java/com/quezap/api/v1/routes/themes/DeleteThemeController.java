package com.quezap.api.v1.routes.themes;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.ports.themes.RemoveTheme;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUsecase;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import org.jspecify.annotations.Nullable;

@RestController
public class DeleteThemeController {
  private final UsecaseExecutor executor;
  private final RemoveThemeUsecase usecase;

  DeleteThemeController(UsecaseExecutor executor, RemoveThemeUsecase usecase) {
    this.executor = executor;
    this.usecase = usecase;
  }

  @DeleteMapping("apiv1/themes/{id}")
  void delete(@PathVariable("id") ThemeId id, @Nullable Authentication authentication) {
    final var effectiveAuth =
        authentication != null
            ? authentication
            : SecurityContextHolder.getContext().getAuthentication();
    if (effectiveAuth == null || !effectiveAuth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
    }

    final var input = new RemoveTheme.Input(id);

    executor.execute(usecase, input);
  }
}

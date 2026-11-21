package com.quezap.api.v1.routes.themes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.themes.RenameThemeRequest;
import com.quezap.application.ports.themes.RenameTheme;
import com.quezap.application.ports.themes.RenameTheme.RenameThemeUsecase;
import com.quezap.application.ports.themes.RenameTheme.ThemeForbiddenException;
import com.quezap.application.ports.themes.RenameTheme.ThemeInUseException;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
public class RenameThemeController {
  private final UsecaseExecutor executor;
  private final RenameThemeUsecase usecase;
  private final @Nullable UserRepository userRepository;

  RenameThemeController(
      UsecaseExecutor executor, RenameThemeUsecase usecase, QuestionRepository questionRepository) {
    this(executor, usecase, questionRepository, null);
  }

  @Autowired
  RenameThemeController(
      UsecaseExecutor executor,
      RenameThemeUsecase usecase,
      QuestionRepository questionRepository,
      @Nullable UserRepository userRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.userRepository = userRepository;
  }

  @PatchMapping(value = {"apiv1/themes/{id}", "apiv1/themes/rename/{id}"})
  void rename(
      @PathVariable("id") ThemeId id,
      @RequestBody @Valid RenameThemeRequest request,
      @Nullable Authentication authentication) {
    final var effectiveAuth = ThemeAuthHelper.requireAuthentication(authentication);
    final var requesterId = ThemeAuthHelper.requireUserId(effectiveAuth, userRepository);
    final var isAdmin = ThemeAuthHelper.isAdmin(effectiveAuth);

    final var name = request.name();
    final var input = new RenameTheme.Input(id, name, requesterId, isAdmin);

    try {
      executor.execute(usecase, input);
    } catch (ThemeForbiddenException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (ThemeInUseException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
    }
  }
}

package com.quezap.api.v1.routes.themes;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.themes.RenameThemeRequest;
import com.quezap.application.ports.themes.RenameTheme;
import com.quezap.application.ports.themes.RenameTheme.RenameThemeUsecase;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
public class RenameThemeController {
  private final UsecaseExecutor executor;
  private final RenameThemeUsecase usecase;
  private final QuestionRepository questionRepository;

  RenameThemeController(
      UsecaseExecutor executor, RenameThemeUsecase usecase, QuestionRepository questionRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.questionRepository = questionRepository;
  }

  @PatchMapping(value = {"apiv1/themes/{id}", "apiv1/themes/rename/{id}"})
  void rename(
      @PathVariable("id") ThemeId id,
      @RequestBody @Valid RenameThemeRequest request,
      @Nullable Authentication authentication) {
    final var effectiveAuth =
        authentication != null
            ? authentication
            : SecurityContextHolder.getContext().getAuthentication();
    if (effectiveAuth == null || !effectiveAuth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
    }

    if (!isAdmin(effectiveAuth) && questionRepository.countWithThemes(Set.of(id)) > 0L) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Cannot rename a theme that is used by existing questions");
    }

    final var name = request.name();
    final var input = new RenameTheme.Input(id, name);

    executor.execute(usecase, input);
  }

  private boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }
}

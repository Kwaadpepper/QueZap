package com.quezap.api.v1.routes.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.themes.CreateThemeRequest;
import com.quezap.api.v1.dto.request.themes.RenameThemeRequest;
import com.quezap.application.ports.themes.AddTheme;
import com.quezap.application.ports.themes.AddTheme.AddThemeUsecase;
import com.quezap.application.ports.themes.RemoveTheme;
import com.quezap.application.ports.themes.RemoveTheme.RemoveThemeUsecase;
import com.quezap.application.ports.themes.RenameTheme;
import com.quezap.application.ports.themes.RenameTheme.RenameThemeUsecase;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ThemeControllerTest {

  private UsecaseExecutor executor;
  private AddThemeUsecase addUsecase;
  private RenameThemeUsecase renameUsecase;
  private RemoveThemeUsecase removeUsecase;
  private QuestionRepository questionRepository;
  private UserRepository userRepository;

  private AddThemeController addController;
  private RenameThemeController renameController;
  private DeleteThemeController deleteController;

  private final UUID userId = UuidV7.randomUuid();

  @BeforeEach
  void setUp() {
    executor = mock(UsecaseExecutor.class);
    addUsecase = mock(AddThemeUsecase.class);
    renameUsecase = mock(RenameThemeUsecase.class);
    removeUsecase = mock(RemoveThemeUsecase.class);
    questionRepository = mock(QuestionRepository.class);
    userRepository = mock(UserRepository.class);

    addController = new AddThemeController(executor, addUsecase, userRepository);
    renameController =
        new RenameThemeController(executor, renameUsecase, questionRepository, userRepository);
    deleteController = new DeleteThemeController(executor, removeUsecase, userRepository);

    final var user =
        User.hydrate(
            new UserId(userId),
            "Teacher",
            new CredentialId(UuidV7.randomUuid()),
            TimelinePoint.now());
    when(userRepository.find(new UserId(userId))).thenReturn(Optional.of(user));

    final var auth = new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void addThemeShouldAttachAuthenticatedCreatedBy() {
    final var themeId = new ThemeId(UuidV7.randomUuid());
    when(executor.execute(any(), any(AddTheme.Input.class)))
        .thenReturn(new AddTheme.Output(themeId));

    addController.add(new CreateThemeRequest(new ThemeName("Physics")));

    final var captor = ArgumentCaptor.forClass(AddTheme.Input.class);
    verify(executor).execute(any(), captor.capture());
    assertThat(captor.getValue().createdBy()).isEqualTo(new UserId(userId));
  }

  @Test
  void renameThemeShouldMapForbiddenAndConflictAndPassAdminFlag() {
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var req = new RenameThemeRequest(new ThemeName("Astrophysics"));

    when(executor.execute(any(), any(RenameTheme.Input.class)))
        .thenThrow(new RenameTheme.ThemeForbiddenException("Forbidden"))
        .thenThrow(new RenameTheme.ThemeInUseException("In use"))
        .thenReturn(new RenameTheme.Output.ThemeRenamed());

    assertThatThrownBy(() -> renameController.rename(themeId, req, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

    assertThatThrownBy(() -> renameController.rename(themeId, req, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    final var adminAuth =
        new UsernamePasswordAuthenticationToken(
            userId.toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    renameController.rename(themeId, req, adminAuth);

    final var captor = ArgumentCaptor.forClass(RenameTheme.Input.class);
    verify(executor, org.mockito.Mockito.times(3)).execute(any(), captor.capture());
    assertThat(captor.getAllValues().get(2).isAdmin()).isTrue();
    assertThat(captor.getAllValues().get(2).requesterId()).isEqualTo(new UserId(userId));
  }

  @Test
  void deleteThemeShouldMapForbiddenAndPassAdminFlag() {
    final var themeId = new ThemeId(UuidV7.randomUuid());

    when(executor.execute(any(), any(RemoveTheme.Input.class)))
        .thenThrow(new RemoveTheme.ThemeForbiddenException("Forbidden"))
        .thenReturn(new RemoveTheme.Output.ThemeRemoved());

    assertThatThrownBy(() -> deleteController.delete(themeId, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

    final var adminAuth =
        new UsernamePasswordAuthenticationToken(
            userId.toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    deleteController.delete(themeId, adminAuth);

    final var captor = ArgumentCaptor.forClass(RemoveTheme.Input.class);
    verify(executor, org.mockito.Mockito.times(2)).execute(any(), captor.capture());
    assertThat(captor.getAllValues().get(1).isAdmin()).isTrue();
    assertThat(captor.getAllValues().get(1).requesterId()).isEqualTo(new UserId(userId));
  }
}

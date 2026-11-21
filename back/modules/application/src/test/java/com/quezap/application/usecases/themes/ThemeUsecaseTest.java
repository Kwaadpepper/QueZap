package com.quezap.application.usecases.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.quezap.application.ports.themes.AddTheme;
import com.quezap.application.ports.themes.RemoveTheme;
import com.quezap.application.ports.themes.RenameTheme;
import com.quezap.application.ports.themes.RenameTheme.ThemeInUseException;
import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ThemeUsecaseTest {

  private ThemeRepository themeRepository;
  private QuestionRepository questionRepository;
  private UnitOfWorkEvents unitOfWork;

  private AddThemeHandler addHandler;
  private RenameThemeHandler renameHandler;
  private RemoveThemeHandler removeHandler;

  @BeforeEach
  void setUp() {
    themeRepository = mock(ThemeRepository.class);
    questionRepository = mock(QuestionRepository.class);
    unitOfWork = mock(UnitOfWorkEvents.class);

    addHandler = new AddThemeHandler(themeRepository);
    renameHandler = new RenameThemeHandler(themeRepository, questionRepository);
    removeHandler = new RemoveThemeHandler(themeRepository, questionRepository);
  }

  @Test
  void addThemeShouldPersistCreatedBy() {
    final var creator = new UserId(UuidV7.randomUuid());
    final var name = new ThemeName("Astronomy");
    when(themeRepository.findByName(name)).thenReturn(Optional.empty());

    addHandler.handle(new AddTheme.Input(name, creator), unitOfWork);

    final var captor = ArgumentCaptor.forClass(Theme.class);
    verify(themeRepository).persist(captor.capture());
    assertThat(captor.getValue().getCreatedBy()).isEqualTo(creator);
  }

  @Test
  void renameThemeShouldRejectNonOwnerAndNullCreatorBeforeConflictCheck() {
    final var owner = new UserId(UuidV7.randomUuid());
    final var attacker = new UserId(UuidV7.randomUuid());
    final var ownedThemeId = new ThemeId(UuidV7.randomUuid());
    final var seededThemeId = new ThemeId(UuidV7.randomUuid());

    final var ownedTheme = Theme.hydrate(ownedThemeId, new ThemeName("Owned"), owner);
    final var seededTheme = Theme.hydrate(seededThemeId, new ThemeName("Seeded"), null);

    when(themeRepository.find(ownedThemeId)).thenReturn(Optional.of(ownedTheme));
    when(themeRepository.find(seededThemeId)).thenReturn(Optional.of(seededTheme));
    when(questionRepository.countWithThemes(Set.of(ownedThemeId))).thenReturn(3L);

    assertThatThrownBy(
            () ->
                renameHandler.handle(
                    new RenameTheme.Input(ownedThemeId, new ThemeName("Hijacked"), attacker, false),
                    unitOfWork))
        .isInstanceOf(RenameTheme.ThemeForbiddenException.class);

    assertThatThrownBy(
            () ->
                renameHandler.handle(
                    new RenameTheme.Input(
                        seededThemeId, new ThemeName("Hijacked2"), attacker, false),
                    unitOfWork))
        .isInstanceOf(RenameTheme.ThemeForbiddenException.class);

    verify(themeRepository, never()).persist(any());
  }

  @Test
  void renameThemeShouldRejectOwnerWhenUsedAndAllowAdmin() {
    final var owner = new UserId(UuidV7.randomUuid());
    final var admin = new UserId(UuidV7.randomUuid());
    final var themeId = new ThemeId(UuidV7.randomUuid());
    final var seededThemeId = new ThemeId(UuidV7.randomUuid());

    final var ownedTheme = Theme.hydrate(themeId, new ThemeName("Owned"), owner);
    final var seededTheme = Theme.hydrate(seededThemeId, new ThemeName("Seeded"), null);

    when(themeRepository.find(themeId)).thenReturn(Optional.of(ownedTheme));
    when(themeRepository.find(seededThemeId)).thenReturn(Optional.of(seededTheme));
    when(questionRepository.countWithThemes(Set.of(themeId))).thenReturn(1L);

    assertThatThrownBy(
            () ->
                renameHandler.handle(
                    new RenameTheme.Input(themeId, new ThemeName("Updated"), owner, false),
                    unitOfWork))
        .isInstanceOf(ThemeInUseException.class);

    renameHandler.handle(
        new RenameTheme.Input(seededThemeId, new ThemeName("AdminUpdated"), admin, true),
        unitOfWork);

    verify(themeRepository).persist(seededTheme);
    assertThat(seededTheme.getName()).isEqualTo(new ThemeName("AdminUpdated"));
  }

  @Test
  void removeThemeShouldEnforceStrictOwnershipAndAdminOverride() {
    final var owner = new UserId(UuidV7.randomUuid());
    final var attacker = new UserId(UuidV7.randomUuid());
    final var admin = new UserId(UuidV7.randomUuid());
    final var ownedThemeId = new ThemeId(UuidV7.randomUuid());
    final var seededThemeId = new ThemeId(UuidV7.randomUuid());

    final var ownedTheme = Theme.hydrate(ownedThemeId, new ThemeName("Owned"), owner);
    final var seededTheme = Theme.hydrate(seededThemeId, new ThemeName("Seeded"), null);

    when(themeRepository.find(ownedThemeId)).thenReturn(Optional.of(ownedTheme));
    when(themeRepository.find(seededThemeId)).thenReturn(Optional.of(seededTheme));
    when(questionRepository.countWithThemes(any())).thenReturn(0L);

    assertThatThrownBy(
            () ->
                removeHandler.handle(
                    new RemoveTheme.Input(ownedThemeId, attacker, false), unitOfWork))
        .isInstanceOf(RemoveTheme.ThemeForbiddenException.class);

    assertThatThrownBy(
            () ->
                removeHandler.handle(
                    new RemoveTheme.Input(seededThemeId, attacker, false), unitOfWork))
        .isInstanceOf(RemoveTheme.ThemeForbiddenException.class);

    removeHandler.handle(new RemoveTheme.Input(ownedThemeId, owner, false), unitOfWork);
    removeHandler.handle(new RemoveTheme.Input(seededThemeId, admin, true), unitOfWork);

    verify(themeRepository).delete(ownedTheme);
    verify(themeRepository).delete(seededTheme);
  }
}

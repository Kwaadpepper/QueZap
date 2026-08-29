package com.quezap.infrastructure.adapter.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.infrastructure.adapter.persistence.jpa.adapters.ThemeRepositoryJpaAdapter;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.ThemeJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.ThemeJpaRepository;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ThemeRepositoryJpaAdapterTest {

  private ThemeJpaRepository themeJpaRepository;
  private ThemeJpaMapper themeJpaMapper;
  private ThemeRepositoryJpaAdapter adapter;

  @BeforeEach
  void setUp() {
    themeJpaRepository = Mockito.mock(ThemeJpaRepository.class);
    themeJpaMapper = new ThemeJpaMapper();
    adapter = new ThemeRepositoryJpaAdapter(themeJpaRepository, themeJpaMapper);
  }

  @Test
  void shouldPersistTheme() {
    final var theme = new Theme(new ThemeName("Geography"));

    adapter.persist(theme);

    verify(themeJpaRepository).save(any(ThemeJpaEntity.class));
  }

  @Test
  void shouldFindThemeById() {
    final var id = UuidV7.randomUuid();
    final var entity = new ThemeJpaEntity(id, "Geography", Instant.now());

    when(themeJpaRepository.findById(id)).thenReturn(Optional.of(entity));

    final var result = adapter.find(new ThemeId(id));

    assertThat(result).isPresent();
    assertThat(result.get().getName().value()).isEqualTo("Geography");
  }

  @Test
  void shouldFindThemeByName() {
    final var id = UuidV7.randomUuid();
    final var entity = new ThemeJpaEntity(id, "Geography", Instant.now());

    when(themeJpaRepository.findByName("Geography")).thenReturn(Optional.of(entity));

    final var result = adapter.findByName(new ThemeName("Geography"));

    assertThat(result).isPresent();
    assertThat(result.get().getId().value()).isEqualTo(id);
  }

  @Test
  void shouldPaginateSearching() {
    final var entity = new ThemeJpaEntity(UuidV7.randomUuid(), "History", Instant.now());
    final var page = new PageImpl<>(List.of(entity));

    when(themeJpaRepository.findByNameContainingIgnoreCase(eq("Hist"), any(Pageable.class)))
        .thenReturn(page);

    final var result = adapter.paginateSearching(Pagination.firstPage(10), new SearchQuery("Hist"));

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).name().value()).isEqualTo("History");
  }
}

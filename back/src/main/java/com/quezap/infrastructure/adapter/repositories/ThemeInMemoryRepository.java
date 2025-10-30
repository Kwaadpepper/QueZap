package com.quezap.infrastructure.adapter.repositories;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

@Repository
public class ThemeInMemoryRepository implements ThemeRepository {
  private final ConcurrentHashMap<ThemeId, Theme> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Theme find(ThemeId id) {
    return storage.get(id);
  }

  @Override
  public void save(Theme entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void update(Theme entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Theme entity) {
    storage.remove(entity.getId());
  }

  @Override
  public @Nullable Theme findByName(ThemeName name) {
    return storage.values().stream()
        .filter(theme -> theme.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public PageOf<Theme> paginate(Pagination pagination) {
    final var allQuestions = storage.values().stream().toList();

    return paginateEntities(pagination, allQuestions);
  }

  @Override
  public PageOf<Theme> paginateSearching(Pagination pagination, SearchQuery search) {
    final var filteredQuestions =
        storage.values().stream()
            .filter(q -> stringLike(q.getName().value(), search.value()))
            .toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  private PageOf<Theme> paginateEntities(Pagination pagination, List<Theme> themes) {
    final var totalItems = themes.size();
    final var fromIndex = ((pagination.pageNumber() - 1) * pagination.pageSize());

    if (fromIndex >= totalItems) {
      return PageOf.empty(pagination);
    }

    themes.sort(createdAtComparator());

    final var toIndex = Math.min(fromIndex + pagination.pageSize(), totalItems);
    final var pageItems = themes.subList((int) fromIndex, (int) toIndex);

    return PageOf.of(pagination, pageItems, (long) totalItems);
  }

  private Boolean stringLike(String haystack, String find) {
    return haystack.toLowerCase(Locale.ROOT).indexOf(find.toLowerCase(Locale.ROOT)) != -1;
  }

  private Comparator<Theme> createdAtComparator() {
    return Comparator.comparing(Theme::getCreatedAt);
  }
}

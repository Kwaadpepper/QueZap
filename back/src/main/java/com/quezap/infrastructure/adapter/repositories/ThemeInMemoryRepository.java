package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.ThemeRepository;

@Repository
public class ThemeInMemoryRepository implements ThemeRepository {
  private final ConcurrentHashMap<ThemeId, Theme> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<Theme> find(ThemeId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void persist(Theme entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Theme entity) {
    storage.remove(entity.getId());
  }

  @Override
  public Optional<Theme> findByName(ThemeName name) {
    return storage.values().stream().filter(theme -> theme.getName().equals(name)).findFirst();
  }

  public <T> List<T> mapWith(Function<Theme, T> mapper) {
    return storage.values().stream().map(theme -> mapper.apply(clone(theme))).toList();
  }

  private Theme clone(Theme theme) {
    return Theme.hydrate(theme.getId(), theme.getName());
  }
}

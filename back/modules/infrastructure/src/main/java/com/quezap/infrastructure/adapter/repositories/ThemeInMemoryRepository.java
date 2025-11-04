package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.ThemeRepository;
import com.quezap.infrastructure.adapter.spi.DataSource;

@Repository("themeInMemoryRepository")
public class ThemeInMemoryRepository implements ThemeRepository, DataSource<Theme> {
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

  @Override
  public List<Theme> getAll() {
    return storage.values().stream().<Theme>map(this::clone).toList();
  }

  private Theme clone(Theme theme) {
    return Theme.hydrate(theme.getId(), theme.getName());
  }
}

package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.port.repositories.ThemeRepository;

import org.jspecify.annotations.Nullable;

@Repository
public class ThemeInMemoryRepository implements ThemeRepository {
  private final ConcurrentHashMap<UUID, Theme> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Theme find(UUID id) {
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
}

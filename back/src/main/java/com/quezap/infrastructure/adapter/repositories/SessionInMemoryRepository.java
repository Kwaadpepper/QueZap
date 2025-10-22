package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.port.repositories.SessionRepository;

import org.eclipse.jdt.annotation.Nullable;

@Repository
public class SessionInMemoryRepository implements SessionRepository {
  private final ConcurrentHashMap<UUID, Session> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Session find(UUID id) {
    return storage.get(id);
  }

  @Override
  public void save(Session entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void update(Session entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Session entity) {
    storage.remove(entity.getId());
  }

  @Override
  public @Nullable Session findByCode(SessionCode code) {
    return storage.values().stream()
        .filter(session -> session.getCode().equals(code))
        .findFirst()
        .orElse(null);
  }

  @Override
  public @Nullable Session latestByCode() {
    return storage.values().stream()
        .max(
            (a, b) -> {
              final var s1Code = a.getCode();
              final var s2Code = b.getCode();

              return s1Code.value().compareTo(s2Code.value());
            })
        .orElse(null);
  }
}

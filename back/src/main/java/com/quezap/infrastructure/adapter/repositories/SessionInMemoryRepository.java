package com.quezap.infrastructure.adapter.repositories;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.port.repositories.SessionRepository;

import org.jspecify.annotations.Nullable;

@Repository
public class SessionInMemoryRepository implements SessionRepository {
  private final ConcurrentHashMap<SessionId, Session> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Session find(SessionId id) {
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
  public @Nullable Session findByNumber(SessionNumber number) {
    return storage.values().stream()
        .filter(session -> session.getNumber().equals(number))
        .findFirst()
        .orElse(null);
  }

  @Override
  public @Nullable Session latestByNumber() {
    return storage.values().stream()
        .max(
            (a, b) -> {
              final var s1Number = a.getNumber();
              final var s2Number = b.getNumber();

              return s1Number.value().compareTo(s2Number.value());
            })
        .orElse(null);
  }
}

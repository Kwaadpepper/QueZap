package com.quezap.infrastructure.adapter.repositories;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.ports.repositories.SessionRepository;

@Repository("sessionInMemoryRepository")
public class SessionInMemoryRepository implements SessionRepository {
  private final ConcurrentHashMap<SessionId, Session> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<Session> find(SessionId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void persist(Session entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Session entity) {
    storage.remove(entity.getId());
  }

  @Override
  public Optional<Session> findByNumber(SessionNumber number) {
    return storage.values().stream()
        .filter(session -> session.getNumber().equals(number))
        .findFirst();
  }

  @Override
  public Optional<Session> latestByNumber() {
    return storage.values().stream().max(numberComparator());
  }

  private Comparator<Session> numberComparator() {
    return (a, b) -> {
      final var s1Number = a.getNumber();
      final var s2Number = b.getNumber();

      return s1Number.value().compareTo(s2Number.value());
    };
  }
}

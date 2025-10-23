package com.quezap.infrastructure.adapter.repositories;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.port.repositories.QuestionRepository;

import org.jspecify.annotations.Nullable;

@Repository
public class QuestionInMemoryRepository implements QuestionRepository {
  private final ConcurrentHashMap<UUID, Question> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Question find(UUID id) {
    return storage.get(id);
  }

  @Override
  public void save(Question entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void update(Question entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Question entity) {
    storage.remove(entity.getId());
  }
}

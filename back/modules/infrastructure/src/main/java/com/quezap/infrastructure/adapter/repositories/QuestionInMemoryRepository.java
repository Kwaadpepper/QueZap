package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.repositories.QuestionRepository;
import com.quezap.infrastructure.adapter.spi.DataSource;

@Repository("questionInMemoryRepository")
public class QuestionInMemoryRepository implements QuestionRepository, DataSource<Question> {
  private final ConcurrentHashMap<QuestionId, Question> storage = new ConcurrentHashMap<>();

  @Override
  public long countWithThemes(Set<ThemeId> themeIds) {
    return storage.values().stream()
        .filter(question -> themeIds.contains(question.getTheme()))
        .count();
  }

  @Override
  public Optional<Question> find(QuestionId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void persist(Question entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Question entity) {
    storage.remove(entity.getId());
  }

  @Override
  public List<Question> getAll() {
    return storage.values().stream().<Question>map(this::clone).toList();
  }

  private Question clone(Question question) {
    return Question.hydrate(
        question.getId(),
        question.getType(),
        question.getValue(),
        question.getPicture(),
        question.getTheme(),
        Set.copyOf(question.getAnswers()),
        question.getUpdatedAt());
  }
}

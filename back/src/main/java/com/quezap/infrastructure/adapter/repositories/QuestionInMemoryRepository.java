package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.infrastructure.adapter.spi.QuestionDataSource;

@Repository("questionInMemoryRepository")
public class QuestionInMemoryRepository implements QuestionRepository, QuestionDataSource {
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
  public <T> List<T> mapAll(Function<Question, T> mapper) {
    return storage.values().stream().map(question -> mapper.apply(clone(question))).toList();
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

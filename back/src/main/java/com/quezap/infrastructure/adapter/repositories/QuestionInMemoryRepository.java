package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.QuestionRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

@Repository
public class QuestionInMemoryRepository implements QuestionRepository {
  private final ConcurrentHashMap<QuestionId, Question> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable Question find(QuestionId id) {
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

  @Override
  public PageOf<Question> paginate(Pagination pagination) {
    final var allQuestions = storage.values().stream().toList();

    return paginate(pagination, allQuestions);
  }

  @Override
  public PageOf<Question> paginateWithThemes(Pagination pagination, Set<ThemeId> themes) {
    final var filteredQuestions =
        storage.values().stream().filter(q -> themes.contains(q.getTheme())).toList();

    return paginate(pagination, filteredQuestions);
  }

  @Override
  public PageOf<Question> paginateSearching(Pagination pagination, SearchQuery search) {
    final var filteredQuestions =
        storage.values().stream().filter(q -> stringLike(q.getValue(), search.value())).toList();

    return paginate(pagination, filteredQuestions);
  }

  @Override
  public PageOf<Question> paginateSearchingWithThemes(
      Pagination pagination, SearchQuery search, Set<ThemeId> themes) {
    final var filteredQuestions =
        storage.values().stream()
            .filter(q -> themes.contains(q.getTheme()) && stringLike(q.getValue(), search.value()))
            .toList();

    return paginate(pagination, filteredQuestions);
  }

  private PageOf<Question> paginate(Pagination pagination, List<Question> questions) {
    final var totalItems = questions.size();
    final var fromIndex = ((pagination.pageNumber() - 1) * pagination.pageSize());

    if (fromIndex >= totalItems) {
      return PageOf.empty(pagination);
    }

    final var toIndex = Math.min(fromIndex + pagination.pageSize(), totalItems);
    final var pageItems = questions.subList((int) fromIndex, (int) toIndex);

    return PageOf.of(pagination, pageItems, (long) totalItems);
  }

  private Boolean stringLike(String haystack, String find) {
    return haystack.toLowerCase(Locale.getDefault()).indexOf(find.toLowerCase(Locale.getDefault()))
        != -1;
  }
}

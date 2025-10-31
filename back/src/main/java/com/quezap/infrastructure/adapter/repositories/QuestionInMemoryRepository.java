package com.quezap.infrastructure.adapter.repositories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

@Repository
public class QuestionInMemoryRepository implements QuestionRepository {
  private final ConcurrentHashMap<QuestionId, Question> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<Question> find(QuestionId id) {
    return Optional.ofNullable(storage.get(id));
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

    return paginateEntities(pagination, allQuestions);
  }

  @Override
  public PageOf<Question> paginateWithThemes(Pagination pagination, Set<ThemeId> themes) {
    final var filteredQuestions =
        storage.values().stream().filter(q -> themes.contains(q.getTheme())).toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  @Override
  public PageOf<Question> paginateSearching(Pagination pagination, SearchQuery search) {
    final var filteredQuestions =
        storage.values().stream().filter(q -> stringLike(q.getValue(), search.value())).toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  @Override
  public PageOf<Question> paginateSearchingWithThemes(
      Pagination pagination, SearchQuery search, Set<ThemeId> themes) {
    final var filteredQuestions =
        storage.values().stream()
            .filter(q -> themes.contains(q.getTheme()) && stringLike(q.getValue(), search.value()))
            .toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  private PageOf<Question> paginateEntities(Pagination pagination, List<Question> questions) {
    final var orderableList = new ArrayList<>(questions);
    final var totalItems = orderableList.size();
    final var fromIndex = ((pagination.pageNumber() - 1) * pagination.pageSize());

    if (fromIndex >= totalItems) {
      return PageOf.empty(pagination);
    }

    orderableList.sort(createdAtComparator());

    final var toIndex = Math.min(fromIndex + pagination.pageSize(), totalItems);
    final var pageItems = orderableList.subList((int) fromIndex, (int) toIndex);

    return PageOf.of(pagination, pageItems, (long) totalItems);
  }

  private Boolean stringLike(String haystack, String find) {
    return haystack.toLowerCase(Locale.ROOT).indexOf(find.toLowerCase(Locale.ROOT)) != -1;
  }

  private Comparator<Question> createdAtComparator() {
    return Comparator.comparing(Question::getCreatedAt);
  }
}

package com.quezap.infrastructure.adapter.directories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.directories.QuestionDirectory;
import com.quezap.domain.port.directories.views.QuestionView;
import com.quezap.infrastructure.adapter.repositories.QuestionInMemoryRepository;
import com.quezap.infrastructure.anotations.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Directory
public class QuestionInMemoryDirectory implements QuestionDirectory {
  private final QuestionInMemoryRepository questionRepository;

  public QuestionInMemoryDirectory(QuestionInMemoryRepository questionRepository) {
    this.questionRepository = questionRepository;
  }

  private List<QuestionView> getAllQuestions() {
    return questionRepository.<QuestionView>mapWith(
        question ->
            new QuestionView(
                question.getId(),
                question.getValue(),
                question.getTheme(),
                question.getCreatedAt()));
  }

  @Override
  public PageOf<QuestionView> paginate(Pagination pagination) {
    final var allQuestions = getAllQuestions();

    return paginateEntities(pagination, allQuestions);
  }

  @Override
  public PageOf<QuestionView> paginateWithThemes(Pagination pagination, Set<ThemeId> themes) {
    final var filteredQuestions =
        getAllQuestions().stream().filter(q -> themes.contains(q.theme())).toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  @Override
  public PageOf<QuestionView> paginateSearching(Pagination pagination, SearchQuery search) {
    final var filteredQuestions =
        getAllQuestions().stream().filter(q -> stringLike(q.question(), search.value())).toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  @Override
  public PageOf<QuestionView> paginateSearchingWithThemes(
      Pagination pagination, SearchQuery search, Set<ThemeId> themes) {
    final var filteredQuestions =
        getAllQuestions().stream()
            .filter(q -> themes.contains(q.theme()) && stringLike(q.question(), search.value()))
            .toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  private PageOf<QuestionView> paginateEntities(
      Pagination pagination, List<QuestionView> questions) {
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

  private Comparator<QuestionView> createdAtComparator() {
    return Comparator.comparing(QuestionView::createdAt);
  }
}

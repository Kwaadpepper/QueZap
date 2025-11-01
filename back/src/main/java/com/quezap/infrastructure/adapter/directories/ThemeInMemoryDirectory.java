package com.quezap.infrastructure.adapter.directories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.port.directories.ThemeDirectory;
import com.quezap.domain.port.directories.views.ThemeView;
import com.quezap.infrastructure.adapter.spi.ThemeDataSource;
import com.quezap.infrastructure.anotations.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Directory
public class ThemeInMemoryDirectory implements ThemeDirectory {
  private final ThemeDataSource themeDataSource;

  public ThemeInMemoryDirectory(
      @Qualifier("themeInMemoryRepository") ThemeDataSource themeDataSource) {
    this.themeDataSource = themeDataSource;
  }

  private List<ThemeView> getAllThemes() {
    return themeDataSource.<ThemeView>mapAll(
        theme -> new ThemeView(theme.getId(), theme.getName(), theme.getCreatedAt()));
  }

  @Override
  public PageOf<ThemeView> paginate(Pagination pagination) {
    final var allQuestions = getAllThemes().stream().toList();

    return paginateEntities(pagination, allQuestions);
  }

  @Override
  public PageOf<ThemeView> paginateSearching(Pagination pagination, SearchQuery search) {
    final var filteredQuestions =
        getAllThemes().stream().filter(q -> stringLike(q.name().value(), search.value())).toList();

    return paginateEntities(pagination, filteredQuestions);
  }

  private PageOf<ThemeView> paginateEntities(Pagination pagination, List<ThemeView> themes) {
    final var orderableList = new ArrayList<>(themes);
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

  private Comparator<ThemeView> createdAtComparator() {
    return Comparator.comparing(ThemeView::createdAt);
  }
}

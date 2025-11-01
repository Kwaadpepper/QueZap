package com.quezap.application.usecases.themes;

import java.util.List;

import com.quezap.application.ports.themes.ListThemes.Input;
import com.quezap.application.ports.themes.ListThemes.ListThemesUsecase;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.ports.directories.ThemeDirectory;
import com.quezap.domain.ports.directories.views.ThemeView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListThemesTest {
  private final ThemeDirectory themeDirectory;
  private final ListThemesUsecase usecase;

  public ListThemesTest() {
    this.themeDirectory = MockEntity.mock(ThemeDirectory.class);
    this.usecase = new ListThemesHandler(themeDirectory);
  }

  @Test
  void canListThemes() {
    // WHEN
    var pagination = Pagination.firstPage();
    var input = new Input.PerPage(pagination);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    var themesPage = PageOf.of(pagination, List.<ThemeView>of(), 0L);
    Mockito.when(themeDirectory.paginate(pagination)).thenReturn(themesPage);

    // WHEN
    usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void alListAndSearch() {
    // WHEN
    var pagination = Pagination.firstPage();
    var search = new SearchQuery("pasta");
    var input = new Input.Searching(pagination, search);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    var themesPage = PageOf.of(pagination, List.<ThemeView>of(), 0L);
    Mockito.when(themeDirectory.paginateSearching(pagination, search)).thenReturn(themesPage);

    // WHEN
    usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

package com.quezap.domain.usecases.themes;

import java.util.List;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListThemesTest {
  private final ThemeRepository themeRepository;
  private final ListThemes.Handler listThemesHandler;

  public ListThemesTest() {
    this.themeRepository = Mockito.mock(ThemeRepository.class);
    this.listThemesHandler = new ListThemes.Handler(themeRepository);
  }

  @Test
  void canListThemes() {
    // WHEN
    var pagination = Pagination.firstPage();
    var input = new ListThemes.Input.PerPage(pagination);

    var themesPage = PageOf.of(pagination, List.<Theme>of(), 0L);
    Mockito.when(themeRepository.paginate(pagination)).thenReturn(themesPage);

    // WHEN
    listThemesHandler.handle(input);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void alListAndSearch() {
    // WHEN
    var pagination = Pagination.firstPage();
    var search = new SearchQuery("pasta");
    var input = new ListThemes.Input.Searching(pagination, search);

    var themesPage = PageOf.of(pagination, List.<Theme>of(), 0L);
    Mockito.when(themeRepository.paginateSearching(pagination, search)).thenReturn(themesPage);

    // WHEN
    listThemesHandler.handle(input);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

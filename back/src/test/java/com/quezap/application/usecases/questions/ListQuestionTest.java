package com.quezap.application.usecases.questions;

import java.util.List;
import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.directories.QuestionDirectory;
import com.quezap.domain.port.directories.views.QuestionView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListQuestionTest {
  private final QuestionDirectory questionDirectory;
  private final ListQuestions.Handler listQuestionsHandler;

  public ListQuestionTest() {
    this.questionDirectory = MockEntity.mock(QuestionDirectory.class);
    this.listQuestionsHandler = new ListQuestions.Handler(questionDirectory);
  }

  @Test
  void canListQuestions() {
    // GIVEN
    var pagination = Pagination.firstPage();
    var input = new ListQuestions.Input.PerPage(pagination);

    var questions = List.of(MockEntity.mock(QuestionView.class));
    var questionPage = PageOf.of(pagination, questions, 1L);
    Mockito.when(questionDirectory.paginate(pagination)).thenReturn(questionPage);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    listQuestionsHandler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canListQuestionsAndSearch() {
    // GIVEN
    var pagination = Pagination.firstPage();
    var searchTerm = new SearchQuery("term");
    var input = new ListQuestions.Input.Searching(pagination, searchTerm);

    var questions = List.of(MockEntity.mock(QuestionView.class));
    var questionPage = PageOf.of(pagination, questions, 1L);
    Mockito.when(questionDirectory.paginateSearching(pagination, searchTerm))
        .thenReturn(questionPage);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    listQuestionsHandler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canListQuestionsWithThemes() {
    // GIVEN
    var pagination = Pagination.firstPage();
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var themeIds = Set.of(theme);
    var input = new ListQuestions.Input.WithThemes(pagination, themeIds);

    var questions = List.of(MockEntity.mock(QuestionView.class));
    var questionPage = PageOf.of(pagination, questions, 1L);
    Mockito.when(questionDirectory.paginateWithThemes(pagination, themeIds))
        .thenReturn(questionPage);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    listQuestionsHandler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }

  @Test
  void canListQuestionsWithThemesAndSearch() {
    // GIVEN
    var pagination = Pagination.firstPage();
    var searchTerm = new SearchQuery("term");
    var theme = ThemeId.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var themeIds = Set.of(theme);
    var input = new ListQuestions.Input.SearchingWithThemes(pagination, searchTerm, themeIds);

    var questions = List.of(MockEntity.mock(QuestionView.class));
    var questionPage = PageOf.of(pagination, questions, 1L);
    Mockito.when(questionDirectory.paginateSearchingWithThemes(pagination, searchTerm, themeIds))
        .thenReturn(questionPage);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    listQuestionsHandler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatNoException().isThrownBy(() -> {});
  }
}

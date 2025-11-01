package com.quezap.application.ports.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface ListQuestions {
  sealed interface Input extends UseCaseInput {
    record PerPage(Pagination page) implements Input {}

    record WithThemes(Pagination page, Set<ThemeId> themes) implements Input {}

    record Searching(Pagination page, SearchQuery search) implements Input {}

    record SearchingWithThemes(Pagination page, SearchQuery search, Set<ThemeId> themes)
        implements Input {}

    public static Input perPage(Pagination page) {
      return new PerPage(page);
    }

    public static Input withThemes(Pagination page, Set<ThemeId> themes) {
      return new WithThemes(page, themes);
    }

    public static Input searching(Pagination page, SearchQuery search) {
      return new Searching(page, search);
    }

    public static Input searchingWithThemes(
        Pagination page, SearchQuery search, Set<ThemeId> themes) {
      return new SearchingWithThemes(page, search, themes);
    }
  }

  record Output(PageOf<QuestionDto> value) implements UseCaseOutput {
    public record QuestionDto(QuestionId id, String question, TimelinePoint createdAt) {}
  }

  public interface ListQuestionsUseCase
      extends UseCaseHandler<ListQuestions.Input, ListQuestions.Output> {}
}

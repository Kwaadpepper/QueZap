package com.quezap.application.ports.themes;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface ListThemes {

  sealed interface Input extends UseCaseInput {
    record PerPage(Pagination page) implements Input {}

    record Searching(Pagination page, SearchQuery search) implements Input {}

    static Input perPage(Pagination page) {
      return new PerPage(page);
    }

    static Input searching(Pagination page, SearchQuery search) {
      return new Searching(page, search);
    }
  }

  record Output(PageOf<ThemeDto> value) implements UseCaseOutput {
    public record ThemeDto(ThemeId id, ThemeName name, TimelinePoint createdAt) {}
  }

  public interface ListThemesUsecase extends UseCaseHandler<ListThemes.Input, ListThemes.Output> {}
}

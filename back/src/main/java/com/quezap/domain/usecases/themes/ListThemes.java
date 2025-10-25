package com.quezap.domain.usecases.themes;

import java.time.ZonedDateTime;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.repositories.ThemeRepository;
import com.quezap.domain.usecases.themes.ListThemes.Output.ThemeDto;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public sealed interface ListThemes {
  sealed interface Input extends UseCaseInput {
    record PerPage(Pagination page) implements Input {}

    record Searching(Pagination page, SearchQuery search) implements Input {}

    public static Input perPage(Pagination page) {
      return new PerPage(page);
    }

    public static Input searching(Pagination page, SearchQuery search) {
      return new Searching(page, search);
    }
  }

  record Output(PageOf<ThemeDto> value) implements UseCaseOutput {
    public record ThemeDto(ThemeId id, ThemeName name, ZonedDateTime createdAt) {}
  }

  // * HANDLER

  final class Handler implements UseCaseHandler<Input, Output>, ListThemes {
    private final ThemeRepository themeRepository;

    public Handler(ThemeRepository themeRepository) {
      this.themeRepository = themeRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      return switch (usecaseInput) {
        case Input.PerPage input -> listPerPage(input);
        case Input.Searching input -> listSearching(input);
      };
    }

    private Output listPerPage(Input.PerPage input) {
      final var page = input.page();
      final var questionPage = themeRepository.paginate(page);

      return new Output(questionPage.map(this::toDto));
    }

    private Output listSearching(Input.Searching input) {
      final var page = input.page();
      final var search = input.search();
      final var themePage = themeRepository.paginateSearching(page, search);

      return new Output(themePage.map(this::toDto));
    }

    private ThemeDto toDto(Theme theme) {
      return new ThemeDto(new ThemeId(theme.getId()), theme.getName(), theme.getCreatedAt());
    }
  }
}

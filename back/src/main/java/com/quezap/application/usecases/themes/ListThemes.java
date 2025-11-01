package com.quezap.application.usecases.themes;

import com.quezap.application.usecases.themes.ListThemes.Output.ThemeDto;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.directories.ThemeDirectory;
import com.quezap.domain.port.directories.views.ThemeView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
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
    public record ThemeDto(ThemeId id, ThemeName name, TimelinePoint createdAt) {}
  }

  // * HANDLER

  final class Handler implements UseCaseHandler<Input, Output>, ListThemes {
    private final ThemeDirectory themeDirectory;

    public Handler(ThemeDirectory themeDirectory) {
      this.themeDirectory = themeDirectory;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      return switch (usecaseInput) {
        case Input.PerPage input -> listPerPage(input);
        case Input.Searching input -> listSearching(input);
      };
    }

    private Output listPerPage(Input.PerPage input) {
      final var page = input.page();
      final var questionPage = themeDirectory.paginate(page);

      return new Output(questionPage.<ThemeDto>map(this::toDto));
    }

    private Output listSearching(Input.Searching input) {
      final var page = input.page();
      final var search = input.search();
      final var themePage = themeDirectory.paginateSearching(page, search);

      return new Output(themePage.<ThemeDto>map(this::toDto));
    }

    private ThemeDto toDto(ThemeView theme) {
      return new ThemeDto(theme.id(), theme.name(), theme.createdAt());
    }
  }
}

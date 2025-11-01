package com.quezap.application.usecases.themes;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.themes.ListThemes.Input;
import com.quezap.application.ports.themes.ListThemes.ListThemesUsecase;
import com.quezap.application.ports.themes.ListThemes.Output;
import com.quezap.domain.ports.directories.ThemeDirectory;
import com.quezap.domain.ports.directories.views.ThemeView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
public final class ListThemesHandler implements ListThemesUsecase {
  private final ThemeDirectory themeDirectory;

  public ListThemesHandler(ThemeDirectory themeDirectory) {
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

    return new Output(questionPage.<Output.ThemeDto>map(this::toDto));
  }

  private Output listSearching(Input.Searching input) {
    final var page = input.page();
    final var search = input.search();
    final var themePage = themeDirectory.paginateSearching(page, search);

    return new Output(themePage.<Output.ThemeDto>map(this::toDto));
  }

  private Output.ThemeDto toDto(ThemeView theme) {
    return new Output.ThemeDto(theme.id(), theme.name(), theme.createdAt());
  }
}

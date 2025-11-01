package com.quezap.application.usecases.questions;

import java.util.Set;

import com.quezap.application.usecases.questions.ListQuestions.Output.QuestionDto;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.port.directories.QuestionDirectory;
import com.quezap.domain.port.directories.views.QuestionView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public sealed interface ListQuestions {
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

  // * HANDLER

  final class Handler implements UseCaseHandler<Input, Output>, ListQuestions {
    private final QuestionDirectory questionDirectory;

    public Handler(QuestionDirectory questionRepository) {
      this.questionDirectory = questionRepository;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      return switch (usecaseInput) {
        case Input.PerPage input -> listPerPage(input);
        case Input.WithThemes input -> listWithThemes(input);
        case Input.Searching input -> listSearching(input);
        case Input.SearchingWithThemes input -> listSearchingWithThemes(input);
      };
    }

    private Output listPerPage(Input.PerPage input) {
      final var page = input.page();
      final var questionPage = questionDirectory.paginate(page);

      return new Output(questionPage.<QuestionDto>map(this::toDto));
    }

    private Output listWithThemes(Input.WithThemes input) {
      final var page = input.page();
      final var themes = input.themes();
      final var questionPage = questionDirectory.paginateWithThemes(page, themes);

      return new Output(questionPage.<QuestionDto>map(this::toDto));
    }

    private Output listSearching(Input.Searching input) {
      final var page = input.page();
      final var search = input.search();
      final var questionPage = questionDirectory.paginateSearching(page, search);

      return new Output(questionPage.<QuestionDto>map(this::toDto));
    }

    private Output listSearchingWithThemes(Input.SearchingWithThemes input) {
      final var page = input.page();
      final var search = input.search();
      final var themes = input.themes();
      final var questionPage = questionDirectory.paginateSearchingWithThemes(page, search, themes);

      return new Output(questionPage.<QuestionDto>map(this::toDto));
    }

    private QuestionDto toDto(QuestionView question) {
      return new QuestionDto(question.id(), question.question(), question.createdAt());
    }
  }
}

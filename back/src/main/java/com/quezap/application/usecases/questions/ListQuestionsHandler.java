package com.quezap.application.usecases.questions;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.questions.ListQuestions.Input;
import com.quezap.application.ports.questions.ListQuestions.ListQuestionsUsecase;
import com.quezap.application.ports.questions.ListQuestions.Output;
import com.quezap.domain.ports.directories.QuestionDirectory;
import com.quezap.domain.ports.directories.views.QuestionView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class ListQuestionsHandler implements ListQuestionsUsecase {
  private final QuestionDirectory questionDirectory;

  public ListQuestionsHandler(QuestionDirectory questionRepository) {
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

    return new Output(questionPage.<Output.QuestionDto>map(this::toDto));
  }

  private Output listWithThemes(Input.WithThemes input) {
    final var page = input.page();
    final var themes = input.themes();
    final var questionPage = questionDirectory.paginateWithThemes(page, themes);

    return new Output(questionPage.<Output.QuestionDto>map(this::toDto));
  }

  private Output listSearching(Input.Searching input) {
    final var page = input.page();
    final var search = input.search();
    final var questionPage = questionDirectory.paginateSearching(page, search);

    return new Output(questionPage.<Output.QuestionDto>map(this::toDto));
  }

  private Output listSearchingWithThemes(Input.SearchingWithThemes input) {
    final var page = input.page();
    final var search = input.search();
    final var themes = input.themes();
    final var questionPage = questionDirectory.paginateSearchingWithThemes(page, search, themes);

    return new Output(questionPage.<Output.QuestionDto>map(this::toDto));
  }

  private Output.QuestionDto toDto(QuestionView question) {
    return new Output.QuestionDto(question.id(), question.question(), question.createdAt());
  }
}

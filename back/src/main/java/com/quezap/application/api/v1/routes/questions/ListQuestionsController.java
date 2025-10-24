package com.quezap.application.api.v1.routes.questions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.request.PaginationDto;
import com.quezap.application.api.v1.dto.request.questions.FindQuestionsDto;
import com.quezap.application.api.v1.dto.response.PageOfDto;
import com.quezap.application.api.v1.dto.response.questions.QuestionShortInfoDto;
import com.quezap.domain.usecases.questions.ListQuestions;
import com.quezap.lib.pagination.Pagination;

import jakarta.validation.Valid;

@RestController("questions")
public class ListQuestionsController {
  private final ListQuestions.Handler handler;

  ListQuestionsController(ListQuestions.Handler handler) {
    this.handler = handler;
  }

  @GetMapping("find")
  PageOfDto<QuestionShortInfoDto> find(
      @Valid PaginationDto paginationDto, @Valid FindQuestionsDto queryDto) {
    final var input = toInput(paginationDto, queryDto);
    final var output = handler.handle(input);

    return PageOfDto.fromDomain(output.value(), this::toDto);
  }

  private ListQuestions.Input toInput(PaginationDto paginationDto, FindQuestionsDto queryDto) {
    final var pagination = toDomain(paginationDto);
    final var search = queryDto.search();
    final var themes = queryDto.themes();

    if (search != null && themes != null && !themes.isEmpty()) {
      return new ListQuestions.Input.SearchingWithThemes(pagination, search, themes);
    } else if (search != null) {
      return new ListQuestions.Input.Searching(pagination, search);
    }

    return new ListQuestions.Input.PerPage(pagination);
  }

  private Pagination toDomain(PaginationDto dto) {
    final var page = dto.page();
    final var perPage = dto.perPage();
    final var from = dto.from();
    final var to = dto.to();

    if (page != null && perPage != null) {
      return Pagination.ofPage(page, perPage);
    } else if (from != null && to != null) {
      return Pagination.ofIndexes(from, to);
    }
    return Pagination.firstPage();
  }

  private QuestionShortInfoDto toDto(ListQuestions.Output.QuestionDto question) {
    return new QuestionShortInfoDto(question.question());
  }
}

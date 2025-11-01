package com.quezap.interfaces.api.v1.routes.questions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.ports.questions.ListQuestions;
import com.quezap.application.ports.questions.ListQuestions.ListQuestionsUseCase;
import com.quezap.interfaces.api.v1.dto.request.PaginationDto;
import com.quezap.interfaces.api.v1.dto.request.questions.FindQuestionsDto;
import com.quezap.interfaces.api.v1.dto.response.PageOfDto;
import com.quezap.interfaces.api.v1.dto.response.questions.QuestionShortInfoDto;
import com.quezap.interfaces.api.v1.mappers.PaginationMapper;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import jakarta.validation.Valid;

@RestController
public class FindQuestionsController {
  private final UseCaseExecutor executor;
  private final ListQuestionsUseCase usecase;
  private final PaginationMapper paginationMapper;

  FindQuestionsController(
      UseCaseExecutor executor, ListQuestionsUseCase usecase, PaginationMapper paginationMapper) {
    this.executor = executor;
    this.usecase = usecase;
    this.paginationMapper = paginationMapper;
  }

  @GetMapping("apiv1/questions/find")
  PageOfDto<QuestionShortInfoDto> find(
      @Valid PaginationDto paginationDto, @Valid FindQuestionsDto queryDto) {
    final var input = toInput(paginationDto, queryDto);
    final var output = executor.execute(usecase, input);

    return paginationMapper.fromDomain(output.value(), this::toDto);
  }

  private ListQuestions.Input toInput(PaginationDto paginationDto, FindQuestionsDto queryDto) {
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var search = queryDto.search();
    final var themes = queryDto.themes();

    if (search != null && themes != null && !themes.isEmpty()) {
      return new ListQuestions.Input.SearchingWithThemes(pagination, search, themes);
    } else if (search != null) {
      return new ListQuestions.Input.Searching(pagination, search);
    }

    return new ListQuestions.Input.PerPage(pagination);
  }

  private QuestionShortInfoDto toDto(ListQuestions.Output.QuestionDto question) {
    return new QuestionShortInfoDto(question.id().value(), question.question());
  }
}

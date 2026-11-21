package com.quezap.api.v1.routes.questions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.questions.FindQuestionsDto;
import com.quezap.api.v1.dto.response.PageOfDto;
import com.quezap.api.v1.dto.response.questions.QuestionShortInfoDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.application.ports.questions.ListQuestions;
import com.quezap.application.ports.questions.ListQuestions.ListQuestionsUsecase;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
public class FindQuestionsController {
  private final UsecaseExecutor executor;
  private final ListQuestionsUsecase usecase;
  private final PaginationMapper paginationMapper;
  private final @Nullable UserRepository userRepository;

  FindQuestionsController(
      UsecaseExecutor executor, ListQuestionsUsecase usecase, PaginationMapper paginationMapper) {
    this(executor, usecase, paginationMapper, null);
  }

  @Autowired
  FindQuestionsController(
      UsecaseExecutor executor,
      ListQuestionsUsecase usecase,
      PaginationMapper paginationMapper,
      @Nullable UserRepository userRepository) {
    this.executor = executor;
    this.usecase = usecase;
    this.paginationMapper = paginationMapper;
    this.userRepository = userRepository;
  }

  @GetMapping(value = {"apiv1/questions", "apiv1/questions/find"})
  PageOfDto<QuestionShortInfoDto> find(
      @Valid PaginationDto paginationDto, @Valid FindQuestionsDto queryDto) {
    final var auth = QuestionAuthHelper.requireAuthentication(null);
    final var requesterId = QuestionAuthHelper.requireUserId(auth, userRepository);
    final var input = toInput(paginationDto, queryDto, requesterId);
    final var output = executor.execute(usecase, input);

    return paginationMapper.fromDomain(output.value(), this::toDto);
  }

  private ListQuestions.Input toInput(
      PaginationDto paginationDto, FindQuestionsDto queryDto, UserId requesterId) {
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var search = queryDto.search();
    final var themes = queryDto.themes();

    if (search != null && themes != null && !themes.isEmpty()) {
      return new ListQuestions.Input.SearchingWithThemes(pagination, search, themes, requesterId);
    } else if (search != null) {
      return new ListQuestions.Input.Searching(pagination, search, requesterId);
    } else if (themes != null && !themes.isEmpty()) {
      return new ListQuestions.Input.WithThemes(pagination, themes, requesterId);
    }

    return new ListQuestions.Input.PerPage(pagination, requesterId);
  }

  private QuestionShortInfoDto toDto(ListQuestions.Output.QuestionDto question) {
    return new QuestionShortInfoDto(question.id().value(), question.question());
  }
}

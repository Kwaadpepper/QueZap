package com.quezap.interfaces.api.v1.routes.themes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.usecases.themes.ListThemes;
import com.quezap.interfaces.api.v1.dto.request.PaginationDto;
import com.quezap.interfaces.api.v1.dto.request.themes.FindThemesDto;
import com.quezap.interfaces.api.v1.dto.response.PageOfDto;
import com.quezap.interfaces.api.v1.dto.response.themes.ThemeDto;
import com.quezap.interfaces.api.v1.mappers.PaginationMapper;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import jakarta.validation.Valid;

@RestController
public class FindThemeController {
  private final UseCaseExecutor executor;
  private final ListThemes.Handler handler;
  private final PaginationMapper paginationMapper;

  FindThemeController(
      UseCaseExecutor executor, ListThemes.Handler handler, PaginationMapper paginationMapper) {
    this.executor = executor;
    this.handler = handler;
    this.paginationMapper = paginationMapper;
  }

  @GetMapping("apiv1/themes/find")
  PageOfDto<ThemeDto> find(@Valid PaginationDto paginationDto, @Valid FindThemesDto queryDto) {
    final var input = toInput(paginationDto, queryDto);

    final var output = executor.execute(handler, input);

    return paginationMapper.fromDomain(output.value(), this::toDto);
  }

  private ListThemes.Input toInput(PaginationDto paginationDto, FindThemesDto queryDto) {
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var search = queryDto.search();

    if (search != null) {
      return new ListThemes.Input.Searching(pagination, search);
    }

    return new ListThemes.Input.PerPage(pagination);
  }

  private ThemeDto toDto(ListThemes.Output.ThemeDto theme) {
    return new ThemeDto(theme.id().value(), theme.name().value());
  }
}

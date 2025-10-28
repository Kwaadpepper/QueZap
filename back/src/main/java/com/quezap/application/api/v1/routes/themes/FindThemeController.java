package com.quezap.application.api.v1.routes.themes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.request.PaginationDto;
import com.quezap.application.api.v1.dto.request.themes.FindThemesDto;
import com.quezap.application.api.v1.dto.response.PageOfDto;
import com.quezap.application.api.v1.dto.response.themes.ThemeDto;
import com.quezap.application.api.v1.exceptions.BadPaginationException;
import com.quezap.domain.usecases.themes.ListThemes;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;
import com.quezap.lib.pagination.Pagination;

import jakarta.validation.Valid;

@RestController
public class FindThemeController {
  private final UseCaseExecutor executor;
  private final ListThemes.Handler handler;

  FindThemeController(UseCaseExecutor executor, ListThemes.Handler handler) {
    this.executor = executor;
    this.handler = handler;
  }

  @GetMapping("apiv1/themes/find")
  PageOfDto<ThemeDto> find(@Valid PaginationDto paginationDto, @Valid FindThemesDto queryDto) {
    final var input = toInput(paginationDto, queryDto);

    final var output = executor.execute(handler, input);

    return PageOfDto.fromDomain(output.value(), this::toDto);
  }

  private ListThemes.Input toInput(PaginationDto paginationDto, FindThemesDto queryDto) {
    final var pagination = toDomain(paginationDto);
    final var search = queryDto.search();

    if (search != null) {
      return new ListThemes.Input.Searching(pagination, search);
    }

    return new ListThemes.Input.PerPage(pagination);
  }

  private Pagination toDomain(PaginationDto dto) {
    try {
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
    } catch (IllegalDomainStateException e) {
      final var errorMessage = e.getMessage();

      throw new BadPaginationException(errorMessage);
    }
  }

  private ThemeDto toDto(ListThemes.Output.ThemeDto theme) {
    return new ThemeDto(theme.id().value(), theme.name().value());
  }
}

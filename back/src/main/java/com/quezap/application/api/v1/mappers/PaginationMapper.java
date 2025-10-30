package com.quezap.application.api.v1.mappers;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.quezap.application.api.v1.dto.request.PaginationDto;
import com.quezap.application.api.v1.dto.response.PageOfDto;
import com.quezap.application.api.v1.exceptions.BadPaginationException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Component
public class PaginationMapper {

  public Pagination toDomain(PaginationDto dto) {
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

  public <E, T> PageOfDto<T> fromDomain(PageOf<E> paginatedData, Function<E, T> mapper) {

    return new PageOfDto<>(
        paginatedData.items().stream().map(mapper).toList(),
        paginatedData.totalItems(),
        paginatedData.totalPages(),
        paginatedData.currentPage(),
        paginatedData.pageSize(),
        paginatedData.hasNextPage(),
        paginatedData.hasPreviousPage());
  }
}

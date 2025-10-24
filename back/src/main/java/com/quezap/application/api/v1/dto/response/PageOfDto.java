package com.quezap.application.api.v1.dto.response;

import java.util.List;
import java.util.function.Function;

import com.quezap.lib.pagination.PageOf;

public record PageOfDto<T>(
    List<T> data,

    // Métadonnées
    long totalElements,
    long totalPages,
    long page,
    long pageSize,
    boolean hasNext,
    boolean hasPrevious) {
  public static <E, T> PageOfDto<T> fromDomain(PageOf<E> paginatedData, Function<E, T> mapper) {

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

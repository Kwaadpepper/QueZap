package com.quezap.lib.pagination;

import com.quezap.lib.utils.Domain;

public record Pagination(long from, long to) {
  public Pagination {
    Domain.checkDomain(() -> from >= 0, "From index must be non-negative");
    Domain.checkDomain(() -> to >= 0, "To index must be non-negative");
    Domain.checkDomain(() -> to > from, "To index must be greater than from index");
    Domain.checkDomain(() -> (to - from + 1) <= 50, "Page size must be less than or equal to 50");
  }

  public long pageSize() {
    return to - from + 1;
  }

  public long pageNumber() {
    return (from / pageSize()) + 1;
  }

  public static Pagination ofIndexes(long from, long to) {
    return new Pagination(from, to);
  }

  public static Pagination ofPage(long pageNumber, long pageSize) {
    Domain.checkDomain(() -> pageNumber > 0, "Page number must be positive and non-zero");
    Domain.checkDomain(() -> pageSize > 0, "Page size must be positive and non-zero");

    final var fromIndex = (pageNumber - 1) * pageSize;
    final var toIndex = Math.max(pageNumber * pageSize - 1, 1);

    return new Pagination(fromIndex, toIndex);
  }

  public static Pagination firstPage(long pageSize) {
    return Pagination.ofPage(1L, pageSize);
  }

  public static Pagination firstPage() {
    return Pagination.ofPage(1L, 25L);
  }
}

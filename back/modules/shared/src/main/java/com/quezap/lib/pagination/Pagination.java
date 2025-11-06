package com.quezap.lib.pagination;

import com.quezap.lib.utils.Domain;

public record Pagination(long from, long to) {
  public Pagination {
    Domain.checkDomain(() -> from >= 0L, "From index must be non-negative");
    Domain.checkDomain(() -> to >= 0L, "To index must be non-negative");
    Domain.checkDomain(() -> to > from, "To index must be greater than from index");
    Domain.checkDomain(() -> (to - from + 1L) <= 50L, "Page size must be less than or equal to 50");
  }

  public long pageSize() {
    return to - from;
  }

  public long pageNumber() {
    return (from / pageSize()) + 1L;
  }

  public static Pagination ofIndexes(long from, long to) {
    return new Pagination(from, to);
  }

  public static Pagination ofPage(long pageNumber, long pageSize) {
    Domain.checkDomain(() -> pageNumber > 0L, "Page number must be positive and non-zero");
    Domain.checkDomain(() -> pageSize > 0L, "Page size must be positive and non-zero");

    final var fromIndex = (pageNumber - 1L) * pageSize;
    final var toIndex = Math.max(pageNumber * pageSize - 1L, 1L);

    return new Pagination(fromIndex, toIndex);
  }

  public static Pagination ofOffsetAndLimit(long offset, long limit) {
    Domain.checkDomain(() -> offset >= 0L, "Offset must be non-negative");
    Domain.checkDomain(() -> limit > 0L, "Limit must be positive and non-zero");

    final var fromIndex = offset;
    final var toIndex = Math.max(offset + limit - 1L, 1L);

    return new Pagination(fromIndex, toIndex);
  }

  public static Pagination firstPage(long pageSize) {
    return Pagination.ofPage(1L, pageSize);
  }

  public static Pagination firstPage() {
    return Pagination.ofPage(1L, 25L);
  }
}

package com.quezap.lib.pagination;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public class PageOf<T> {
  private final Pagination pageRequest;
  private final List<T> items;
  private final long totalItems;

  public static <T> PageOf<T> empty(Pagination pagination) {
    return new PageOf<>(pagination, List.of(), 0L);
  }

  public static <T> PageOf<T> of(Pagination pagination, List<T> items, Long totalItems) {
    return new PageOf<>(pagination, items, totalItems);
  }

  public PageOf(Pagination pagination, List<T> items, Long totalItems) {
    this.pageRequest = pagination;
    this.items = List.copyOf(items);
    this.totalItems = totalItems;
  }

  public List<T> items() {
    return items;
  }

  public Long totalItems() {
    return totalItems;
  }

  public Long currentPage() {
    return pageRequest.pageNumber();
  }

  public Long pageSize() {
    return pageRequest.pageSize();
  }

  public Long totalPages() {
    return (long) Math.ceil((double) totalItems / pageRequest.pageSize());
  }

  public boolean hasNextPage() {
    return pageRequest.pageNumber() < totalPages();
  }

  public boolean hasPreviousPage() {
    return pageRequest.pageNumber() > 1;
  }

  public @Nullable Long nextPage() {
    return hasNextPage() ? pageRequest.pageNumber() + 1 : null;
  }

  public @Nullable Long previousPage() {
    return hasPreviousPage() ? pageRequest.pageNumber() - 1 : null;
  }

  public <U> PageOf<U> map(Function<T, U> mapper) {
    final var mappedItems = items.stream().map(mapper).toList();
    return new PageOf<>(pageRequest, mappedItems, totalItems);
  }

  @Override
  public String toString() {
    return "PageOf{"
        + "pageRequest="
        + pageRequest
        + ", items="
        + items
        + ", totalItems="
        + totalItems
        + '}';
  }
}

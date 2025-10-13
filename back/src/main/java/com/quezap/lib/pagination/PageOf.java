package com.quezap.lib.pagination;

import java.util.List;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

public class PageOf<T> {
  private final PageRequest pageRequest;
  private final List<T> items;
  private final long totalItems;

  public static <T> PageOf<T> empty(PageRequest pageRequest) {
    return new PageOf<>(pageRequest, List.of(), 0L);
  }

  public PageOf(PageRequest pageRequest, List<T> items, Long totalItems) {
    this.pageRequest = pageRequest;
    this.items = List.copyOf(items);
    this.totalItems = totalItems;
  }

  public List<T> getItems() {
    return items;
  }

  public Long getTotalItems() {
    return totalItems;
  }

  public Long getCurrentPage() {
    return pageRequest.pageNumber();
  }

  public Long getPageSize() {
    return pageRequest.pageSize();
  }

  public Long getTotalPages() {
    return (long) Math.ceil((double) totalItems / pageRequest.pageSize());
  }

  public boolean hasNextPage() {
    return pageRequest.pageNumber() < getTotalPages();
  }

  public boolean hasPreviousPage() {
    return pageRequest.pageNumber() > 1;
  }

  public @Nullable Long getNextPageNumber() {
    return hasNextPage() ? pageRequest.pageNumber() + 1 : null;
  }

  public @Nullable Long getPreviousPageNumber() {
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

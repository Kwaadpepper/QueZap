package com.quezap.lib.pagination;

import java.util.stream.Stream;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PageOfTest {

  @Test
  void canInstanciatePageRequestWithPageValues() {
    // GIVEN
    var pageNumber = 1L;
    var pageSize = 1L;

    // WHEN
    var page = Pagination.ofPage(pageNumber, pageSize);

    // THEN
    Assertions.assertEquals(0L, page.from());
    Assertions.assertEquals(1L, page.to());
  }

  @Test
  void canInstanciatePageRequestWithPageValuesAgain() {
    // GIVEN
    var pageNumber = 2L;
    var pageSize = 25L;

    // WHEN
    var page = Pagination.ofPage(pageNumber, pageSize);

    // THEN
    Assertions.assertEquals(25L, page.from());
    Assertions.assertEquals(49L, page.to());
  }

  @Test
  void canInstanciatePageRequestWithIndexes() {
    // GIVEN
    var from = 10L;
    var to = 25L;

    // WHEN
    var page = Pagination.ofIndexes(from, to);

    // THEN
    Assertions.assertEquals(from, page.from());
    Assertions.assertEquals(to, page.to());
  }

  // --- Tests d'arguments invalides pour ofPage ---

  @ParameterizedTest
  @MethodSource("invalidPageValues")
  void cannotInstanciatePageRequestWithInvalidPageArguments(long pageNumber, long pageSize) {
    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          Pagination.ofPage(pageNumber, pageSize);
        });
  }

  private static Stream<Arguments> invalidPageValues() {
    return Stream.of(
        // Zero page number.
        Arguments.of(0L, 1L),
        // Negative page number.
        Arguments.of(-1L, 1L),
        // Zero page size.
        Arguments.of(1L, 0L),
        // Negative page size.
        Arguments.of(1L, -1L),
        // Exceeding max page size (50 est max, 51 est en trop)
        Arguments.of(1L, 51L));
  }

  @ParameterizedTest
  @MethodSource("invalidIndexes")
  void cannotInstanciatePageRequestWithInvalidIndexes(long from, long to) {
    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          Pagination.ofIndexes(from, to);
        });
  }

  private static Stream<Arguments> invalidIndexes() {
    return Stream.of(
        // Negative from index.
        Arguments.of(-1L, 10L),
        // Negative to index.
        Arguments.of(0L, -10L),
        // To index less than from index (to > from requis)
        Arguments.of(10L, 5L),
        // Exceeding max page size (taille = 50 + 1 = 51)
        Arguments.of(0L, 50L));
  }

  @Test
  void canInstanciatePageRequestWithMaxPageSize() {
    // GIVEN
    var pageNumber = 1L;
    var pageSize = 50L;

    // WHEN
    var page = Pagination.ofPage(pageNumber, pageSize);

    // THEN
    Assertions.assertEquals(0L, page.from());
    Assertions.assertEquals(49L, page.to());
  }

  @Test
  void canInstanciatePageRequestWithMaxPageSizeIndexes() {
    // GIVEN
    var from = 100L;
    var to = 149L;

    // WHEN
    var page = Pagination.ofIndexes(from, to);

    // THEN
    Assertions.assertEquals(from, page.from());
    Assertions.assertEquals(to, page.to());
  }
}

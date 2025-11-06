package com.quezap.lib.pagination;

import java.util.stream.Stream;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PageOfTest {

  @ParameterizedTest
  @MethodSource("validPageValues")
  void canInstanciatePageRequestWithPageValues(long pageNumber, long pageSize, long from, long to) {
    // WHEN
    var page = Pagination.ofPage(pageNumber, pageSize);

    // THEN
    Assertions.assertEquals(from, page.from());
    Assertions.assertEquals(to, page.to());
  }

  private static Stream<Arguments> validPageValues() {
    return Stream.of(
        // Valid page number and size combinations.
        Arguments.of(1L, 1L, 0L, 1L),
        Arguments.of(1L, 10L, 0L, 9L),
        // ---
        Arguments.of(2L, 10L, 10L, 19L));
  }

  @ParameterizedTest
  @MethodSource("validIndexValues")
  void canInstanciatePageRequestWithIndexes(long from, long to) {
    // WHEN
    Pagination.ofIndexes(from, to);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  private static Stream<Arguments> validIndexValues() {
    return Stream.of(
        // Valid from and to combinations.
        Arguments.of(0L, 1L),
        Arguments.of(0L, 10L),
        // ---
        Arguments.of(10L, 50L));
  }

  @ParameterizedTest
  @MethodSource("validOffsetValues")
  void canInstanciatePageRequestWithOffset(long offset, long limit, long from, long to) {
    // WHEN
    final var page = Pagination.ofOffsetAndLimit(offset, limit);

    // THEN
    Assertions.assertEquals(from, page.from());
    Assertions.assertEquals(to, page.to());
  }

  private static Stream<Arguments> validOffsetValues() {
    return Stream.of(
        // Valid offset and limit combinations.
        Arguments.of(0L, 1L, 0L, 1L),
        Arguments.of(0L, 10L, 0L, 9L),
        // ---
        Arguments.of(10L, 10L, 10L, 19L));
  }

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

  @ParameterizedTest
  @MethodSource("invalidOffsets")
  void cannotInstantiatePageWithRequestWithInvalidOffsetAndLimit(long offset, long limit) {
    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          Pagination.ofOffsetAndLimit(offset, limit);
        });
  }

  private static Stream<Arguments> invalidOffsets() {
    return Stream.of(
        // Negative offset index.
        Arguments.of(-1L, 10L),
        // Negative limit index.
        Arguments.of(0L, -10L),
        // Zero limit index.
        Arguments.of(0L, 0L));
  }
}

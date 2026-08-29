package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RawIdentifierTest {
  private static final String TOO_LONG_IDENTIFIER =
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

  @Test
  void canInstantiate() {
    // GIVEN
    var identifier = "user@example.net";

    // WHEN
    new RawIdentifier(identifier);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "abc", TOO_LONG_IDENTIFIER})
  void cannotInstantiateWithInvalidValue(String identifier) {
    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawIdentifier(identifier));
  }
}

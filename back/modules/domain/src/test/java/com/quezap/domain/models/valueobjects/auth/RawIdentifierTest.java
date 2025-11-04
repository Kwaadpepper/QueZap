package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RawIdentifierTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var identifier = "some-id";

    // WHEN
    new RawIdentifier(identifier);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "abc", "abcdefghijk"})
  void cannotInstantiateWithInvalidValue(String identifier) {
    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawIdentifier(identifier));
  }
}

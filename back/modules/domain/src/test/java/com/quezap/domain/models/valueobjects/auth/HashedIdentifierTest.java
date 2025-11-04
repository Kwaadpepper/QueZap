package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HashedIdentifierTest {
  @Test
  void canInstantiate() {
    // GIVEN
    var id = "some-id";

    // WHEN
    new HashedIdentifier(id);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankId() {
    // GIVEN
    var id = "   ";

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new HashedIdentifier(id);
        });
  }
}

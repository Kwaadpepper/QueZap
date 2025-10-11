package com.quezap.domain.models.valueobjects;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@NonNullByDefault
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

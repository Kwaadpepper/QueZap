package com.quezap.domain.models.valueobjects;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@NonNullByDefault
class HashedPasswordTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var hashedPassword = "$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash";

    // WHEN
    new HashedPassword(hashedPassword);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankValue() {
    // GIVEN
    var hashedPassword = "   ";

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new HashedPassword(hashedPassword);
        });
  }
}

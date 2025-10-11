package com.quezap.domain.models.entities;

import java.util.UUID;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThemeTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var value = "Science";

    // WHEN
    new Theme(value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var value = "Science";

    // WHEN
    Theme.hydrate(id, value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankValue() {
    // GIVEN
    var value = "   ";

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Theme(value);
        });
  }

  @Test
  void cannotInstantiateWithNameTooLong() {
    // GIVEN
    var value = "A".repeat(256);

    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Theme(value);
        });
  }
}

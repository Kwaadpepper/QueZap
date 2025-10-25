package com.quezap.domain.models.entities;

import java.util.UUID;

import com.quezap.domain.models.valueobjects.ThemeName;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThemeTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var value = new ThemeName("Science");

    // WHEN
    new Theme(value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var id = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var value = new ThemeName("Science");

    // WHEN
    Theme.hydrate(id, value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

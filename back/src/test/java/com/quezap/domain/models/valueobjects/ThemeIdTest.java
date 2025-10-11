package com.quezap.domain.models.valueobjects;

import java.util.UUID;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThemeIdTest {

  @Test
  void canInstantiateThemeId() {
    // GIVEN
    var uuid = UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"); // UUIDv7

    // WHEN
    new ThemeId(uuid);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateThemeIdWithNonUuidV7() {
    // GIVEN
    var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"); // UUIDv4
    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new ThemeId(uuid);
        });
  }
}

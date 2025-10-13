package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserIdTest {

  @Test
  void canInstantiateUserId() {
    // GIVEN
    var uuid = UUID.fromString("017f5a80-7e6d-7e6d-0000-000000000000"); // UUIDv7

    // WHEN
    new UserId(uuid);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateUserIdWithNonUuidV7() {
    // GIVEN
    var uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"); // UUIDv4
    // WHEN & THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new UserId(uuid);
        });
  }
}

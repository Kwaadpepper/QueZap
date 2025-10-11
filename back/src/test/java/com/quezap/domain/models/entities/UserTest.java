package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.quezap.domain.exceptions.IllegalDomainStateException;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserTest {
  @Test
  void canInstantiate() {
    // GIVEN
    var name = "some-name";

    // WHEN
    new User(name, ZonedDateTime.now(ZoneId.of("UTC")));

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var name = "some-name";
    var utc = ZonedDateTime.now(ZoneId.of("UTC"));
    var id = UuidV7.randomUuid();

    // WHEN
    User.hydrate(id, name, utc);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankName() {
    // GIVEN
    var name = "   ";
    var utc = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new User(name, utc));
  }

  @Test
  void cannotInstantiateWithNameTooLong() {
    // GIVEN
    var name = "A".repeat(256);
    var utc = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new User(name, utc));
  }
}

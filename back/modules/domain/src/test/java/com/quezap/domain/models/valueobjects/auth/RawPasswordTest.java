package com.quezap.domain.models.valueobjects.auth;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RawPasswordTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var password = "Some-password123!";

    // WHEN
    new RawPassword(password);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstanciateIfTooShort() {
    // GIVEN
    var password = "3.aW";

    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals(
        "Raw password must be at least 8 characters long", exception.getMessage());
  }

  @Test
  void cannotInstanciateIfNoUppercase() {
    // GIVEN
    var password = "some-password123!";

    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals("Raw password must contain mixed case letters", exception.getMessage());
  }

  @Test
  void cannotInstanciateIfNoLowercase() {
    // GIVEN
    var password = "SOME-PASSWORD123!";

    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals("Raw password must contain mixed case letters", exception.getMessage());
  }

  @Test
  void cannotInstanciateIfNoNumber() {
    // GIVEN
    var password = "Some-password!";

    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals("Raw password must contain numbers", exception.getMessage());
  }

  @Test
  void cannotInstanciateIfNoSpecialCharacter() {
    // GIVEN
    var password = "Somepassword123";
    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals("Raw password must contain special characters", exception.getMessage());
  }

  @Test
  void cannotInstanciateIfBlank() {
    // GIVEN
    var password = "   ";

    // WHEN / THEN
    var exception =
        Assertions.assertThrows(IllegalDomainStateException.class, () -> new RawPassword(password));
    Assertions.assertEquals("Raw password cannot be blank", exception.getMessage());
  }
}

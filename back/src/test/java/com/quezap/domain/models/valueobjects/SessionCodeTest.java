package com.quezap.domain.models.valueobjects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionCodeTest {

  @Test
  void canInstantiateSessionCode() {
    // GIVEN
    var code = "A1B2";

    // WHEN
    new SessionCode(code);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @ParameterizedTest
  @ValueSource(strings = {"A1B2C", "a1B2", "A1@2", "   ", "", "A1"})
  void cannotInstantiateSessionCodeWithInvalidValues(String code) {

    // WHEN & THEN
    Assertions.assertThrows(IllegalArgumentException.class, () -> new SessionCode(code));
  }
}

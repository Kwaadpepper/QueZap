package com.quezap.domain.models.valueobjects.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordCandidateTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var password = "some-password";

    // WHEN
    new PasswordCandidate(password);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

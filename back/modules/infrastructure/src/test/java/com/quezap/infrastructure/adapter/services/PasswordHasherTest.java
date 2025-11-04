package com.quezap.infrastructure.adapter.services;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.ports.services.PasswordHasher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PasswordHasherTest {
  private final PasswordHasher passwordHasher;

  PasswordHasherTest() {
    // Test with argon2
    final var passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    this.passwordHasher = new PasswordHasherImpl(passwordEncoder);
  }

  @Test
  void canEncodePassword() {
    // GIVEN
    var rawPassword = new RawPassword("S3cure.P4ssw0rd!!");

    // WHEN
    passwordHasher.hash(rawPassword);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "'S3cure.Passw0rd!!', 'S3cure.Passw0rd!!', 1",
        "'S3cure.Passw0rd!!', 'S3cure.Passw0rd!', 0"
      })
  void canVerifyPassword(String raw, String candidate, Integer result) {
    // GIVEN
    var rawPassword = new RawPassword(raw);
    var hashedPassword = passwordHasher.hash(rawPassword);
    var passwordCandidate = new PasswordCandidate(candidate);

    // WHEN
    var output = passwordHasher.verify(passwordCandidate, hashedPassword);

    // THEN
    Assertions.assertEquals(output, (result == 1));
  }
}

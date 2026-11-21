package com.quezap.infrastructure.adapter.services;

import java.util.Set;

import com.quezap.application.config.ProfanityConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserNameSanitizerTest {

  private final ProfanityConfig profanityConfig;
  private final UserNameSanitizerImpl sanitizer;
  private final Set<String> customWords = Set.of("crotte");

  UserNameSanitizerTest() {
    this.profanityConfig = new ProfanityConfig(customWords);
    this.sanitizer = new UserNameSanitizerImpl(profanityConfig);
  }

  @ParameterizedTest
  @CsvSource({
    "'John Doe','John Doe'",
    "'  John   Doe  ','John Doe'",
    "'     ',''",
    "'',''",
    "'Fuck',''",
    "'fuck',''",
    "'crotte',''",
    "'john shit doe', 'John Doe'",
    "'shit john doe', 'John Doe'",
    "'john doe shit', 'John Doe'",
    "'shit john crotte doe shit', 'John Doe'",
    "'shitjohnshitdoeshit', 'Johndoe'",
    "'shitjohncuntdoeshitcunt', 'Johndoe'",
    "'<script>alert(1)</script>', 'Alert(1)'",
    "'<b>Bob</b>', 'Bob'",
  })
  void canSanitizeUserName(String userName, String expectedSanitized) {
    // WHEN
    final var sanitizedUserName = sanitizer.sanitize(userName);

    // THEN
    Assertions.assertEquals(expectedSanitized, sanitizedUserName);
  }

  @Test
  void terminatesSafelyOnRepeatedOrLeetProfanityInputs() {
    final var repeated = "crotte ".repeat(40) + "Alice";
    final var sanitizedRepeated = sanitizer.sanitize(repeated);
    Assertions.assertEquals("Alice", sanitizedRepeated);

    final var leetInput = "sh1t f0ck Alice";
    final var sanitizedLeet = sanitizer.sanitize(leetInput);
    Assertions.assertNotNull(sanitizedLeet);
  }
}

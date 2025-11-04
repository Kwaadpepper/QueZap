package com.quezap.infrastructure.adapter.services;

import java.util.Set;

import com.quezap.application.config.ProfanityConfig;

import org.junit.jupiter.api.Assertions;
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
  })
  void canSanitizeUserName(String userName, String expectedSanitized) {
    // WHEN
    final var sanitizedUserName = sanitizer.sanitize(userName);

    // THEN
    Assertions.assertEquals(expectedSanitized, sanitizedUserName);
  }
}

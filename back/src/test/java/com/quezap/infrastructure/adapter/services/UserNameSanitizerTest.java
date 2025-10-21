package com.quezap.infrastructure.adapter.services;

import java.util.Set;

import com.quezap.application.config.ProfanityConfig;
import com.quezap.domain.port.services.UserNameSanitizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class UserNameSanitizerTest {
  private final UserNameSanitizer sanitizer;
  private final Set<String> customWords = Set.of("crotte");

  public UserNameSanitizerTest() {
    final var profanityConfig = Mockito.mock(ProfanityConfig.class);

    Mockito.when(profanityConfig.getForbiddenWords()).thenReturn(customWords);

    this.sanitizer = new UserNameSanitizerImpl(profanityConfig);
  }

  @ParameterizedTest
  @CsvSource({
    "'John Doe','John Doe'",
    "'  John   Doe  ','John Doe'",
    "'     ',''",
    "'',''",
    // Sample words to ban, from already hearded ones to random ones
    "'Fuck',''",
    "'fuck',''",
    // custom added word
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

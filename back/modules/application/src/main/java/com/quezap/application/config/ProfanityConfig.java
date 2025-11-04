package com.quezap.application.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfanityConfig {
  private Set<String> customWords;

  public ProfanityConfig(@Value("${profanity.custom-words}") Set<String> customWords) {
    this.customWords = customWords;
  }

  public Set<String> getForbiddenWords() {
    return customWords;
  }
}

package com.quezap.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionCodeConfig {
  private final int length;
  private final long seed;
  private final char[] dictionary;

  public SessionCodeConfig(
      @Value("session-code.length") Integer length,
      @Value("session-code.seed") Long seed,
      @Value("session-code.dictionary") String dictionaryString) {
    this.length = length;
    this.seed = seed;
    this.dictionary = dictionaryString.toCharArray();
  }

  public int getLength() {
    return length;
  }

  public long getSeed() {
    return seed;
  }

  public char[] getDictionary() {
    return dictionary;
  }
}

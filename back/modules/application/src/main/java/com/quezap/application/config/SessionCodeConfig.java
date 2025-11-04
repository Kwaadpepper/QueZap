package com.quezap.application.config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionCodeConfig {
  private final int length;
  private final long seed;
  private final char[] dictionary;

  public SessionCodeConfig(
      @Value("${session-code.length}") Integer length,
      @Value("${session-code.seed}") Long seed,
      @Value("${session-code.dictionary}") String dictionaryString) {
    this.length = length;
    this.seed = seed;
    this.dictionary = dictionaryString.toCharArray();

    Objects.requireNonNull(length, "Session code length must be provided");
    Objects.requireNonNull(seed, "Session code seed must be provided");
    Objects.requireNonNull(dictionaryString, "Session code dictionary must be provided");
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

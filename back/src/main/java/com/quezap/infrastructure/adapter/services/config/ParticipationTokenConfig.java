package com.quezap.infrastructure.adapter.services.config;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParticipationTokenConfig {
  private final int expirationSeconds;

  public ParticipationTokenConfig(
      @Value("${participation.token-expiration-seconds}") Integer expirationSeconds) {
    this.expirationSeconds =
        Objects.requireNonNull(expirationSeconds, "expirationSeconds cannot be null");
  }

  public int expirationSeconds() {
    return expirationSeconds;
  }
}

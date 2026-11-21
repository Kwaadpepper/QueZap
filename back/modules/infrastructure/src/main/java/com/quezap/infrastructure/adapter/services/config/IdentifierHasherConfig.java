package com.quezap.infrastructure.adapter.services.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import org.jspecify.annotations.Nullable;

@Configuration
public class IdentifierHasherConfig {
  public static final String DEFAULT_DEV_PEPPER =
      "7d9e3b1f4c8a2e6d5b0f9a1c3e7d2b4f6a8c0e1d3b5f7a9c2e4d6f8a0b1c3e5d";
  private static final int MIN_PEPPER_BYTES = 32;
  private static final Set<String> NON_PROD_PROFILES = Set.of("dev", "local", "test");

  private final SecretKey hmacKey;

  public IdentifierHasherConfig(String pepper) {
    this(pepper, null);
  }

  @Autowired
  public IdentifierHasherConfig(
      @Value("${app.security.identifier-pepper:" + DEFAULT_DEV_PEPPER + "}") String pepper,
      @Nullable Environment environment) {
    final var nonNullPepper =
        Objects.requireNonNull(pepper, "Identifier pepper must be provided").trim();
    validatePepper(nonNullPepper, environment);
    this.hmacKey = new SecretKeySpec(nonNullPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  private void validatePepper(String rawPepper, @Nullable Environment environment) {
    final var pepperBytes = rawPepper.getBytes(StandardCharsets.UTF_8);
    if (pepperBytes.length < MIN_PEPPER_BYTES) {
      throw new IllegalStateException(
          "Identifier pepper must be at least " + MIN_PEPPER_BYTES + " bytes long");
    }

    if (environment != null
        && isStrictProfile(environment)
        && DEFAULT_DEV_PEPPER.equals(rawPepper)) {
      throw new IllegalStateException(
          "Default development identifier pepper cannot be used outside dev/local/test profiles");
    }
  }

  private boolean isStrictProfile(Environment environment) {
    final var activeProfiles = environment.getActiveProfiles();
    for (final var profile : activeProfiles) {
      if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    if (activeProfiles.length == 0) {
      return false;
    }
    for (final var profile : activeProfiles) {
      if (NON_PROD_PROFILES.contains(profile.toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  public SecretKey hmacKey() {
    return hmacKey;
  }
}

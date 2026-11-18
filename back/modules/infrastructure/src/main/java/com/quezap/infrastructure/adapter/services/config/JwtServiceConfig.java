package com.quezap.infrastructure.adapter.services.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.Nullable;

@Configuration
public class JwtServiceConfig {
  static final String DEFAULT_DEV_KEY =
      "4a3c4e58311739e4a396ad7a38a5454ad65cfde67b0fef9aeb351cbb3c478fef";
  private static final int MIN_KEY_BYTES = 32;
  private static final Set<String> NON_PROD_PROFILES = Set.of("dev", "local", "test");

  private final String jwtIssuer;
  private final SecretKey jwtSigningKey;

  public JwtServiceConfig(String jwtSigningKey, String jwtIssuer) {
    this(jwtSigningKey, jwtIssuer, null);
  }

  @Autowired
  public JwtServiceConfig(
      @Value("${jwt.signing-key}") String jwtSigningKey,
      @Value("${jwt.issuer}") String jwtIssuer,
      @Nullable Environment environment) {
    this.jwtIssuer = Objects.requireNonNull(jwtIssuer, "JWT issuer must be provided");
    final var nonNullKey =
        Objects.requireNonNull(jwtSigningKey, "JWT signing key must be provided");
    validateSigningKey(nonNullKey, environment);
    this.jwtSigningKey = keyFromString(nonNullKey);
  }

  private void validateSigningKey(String rawKey, @Nullable Environment environment) {
    final var keyBytes = rawKey.trim().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException(
          "JWT signing key must be at least " + MIN_KEY_BYTES + " bytes long");
    }

    if (environment != null && isStrictProfile(environment)) {
      if (DEFAULT_DEV_KEY.equals(rawKey.trim())) {
        throw new IllegalStateException(
            "Default development JWT signing key cannot be used outside dev/local/test profiles");
      }
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
      if (NON_PROD_PROFILES.contains(profile.toLowerCase(java.util.Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  private SecretKey keyFromString(String base64String) {
    return Objects.requireNonNull(
        Keys.hmacShaKeyFor(base64String.getBytes(StandardCharsets.UTF_8)),
        "JWT signing key must be provided");
  }

  public String jwtIssuer() {
    return jwtIssuer;
  }

  public SecretKey jwtSigningKey() {
    return jwtSigningKey;
  }
}

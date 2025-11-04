package com.quezap.infrastructure.adapter.services.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtServiceConfig {
  private final String jwtIssuer;
  private final SecretKey jwtSigningKey;

  public JwtServiceConfig(
      @Value("${jwt.signing-key}") String jwtSigningKey, @Value("${jwt.issuer}") String jwtIssuer) {
    this.jwtIssuer = Objects.requireNonNull(jwtIssuer, "JWT issuer must be provided");
    this.jwtSigningKey =
        keyFromString(Objects.requireNonNull(jwtSigningKey, "JWT signing key must be provided"));
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

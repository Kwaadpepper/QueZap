package com.quezap.infrastructure.adapter.services.jwt;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.quezap.infrastructure.adapter.services.config.JwtServiceConfig;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtServiceImpl implements JwtService {
  private static final Logger logger =
      Objects.requireNonNull(LoggerFactory.getLogger(JwtServiceImpl.class));
  private final String jwtIssuer;
  private final SecretKey jwtSigningKey;

  JwtServiceImpl(JwtServiceConfig config) {
    this.jwtIssuer = config.jwtIssuer();
    this.jwtSigningKey = config.jwtSigningKey();
  }

  @Override
  public String generateToken(JwtPayload payload) throws JwtException {
    try {
      @SuppressWarnings("squid:S3252")
      final var expiration = Date.from(Instant.ofEpochSecond(payload.exp()));
      @SuppressWarnings("squid:S3252")
      final var issuedAt = Date.from(Instant.ofEpochSecond(payload.iat()));
      final var notBeforeValue = payload.nbf();
      @SuppressWarnings("squid:S3252")
      final var notBefore =
          notBeforeValue != null ? Date.from(Instant.ofEpochSecond(notBeforeValue)) : null;
      final var jwtIdentifier = payload.jti();
      final var subject = payload.sub();
      final var audience = payload.aud();
      final var customClaims = payload.customClaims();

      final var builder =
          Jwts.builder()
              .header()
              .type("JWT")
              .keyId(null)
              .and()
              .issuer(jwtIssuer)
              .subject(subject)
              .audience()
              .add(audience)
              .and()
              .expiration(expiration)
              .issuedAt(issuedAt)
              .notBefore(notBefore)
              .id(jwtIdentifier)
              .claims(customClaims)
              .signWith(jwtSigningKey);

      return Objects.requireNonNull(builder.compact());

    } catch (
        @SuppressWarnings("squid:S2139")
        Exception e) {
      logger.error("Error generating JWT token", e);
      throw new JwtException("Error generating JWT token", e);
    }
  }

  @Override
  public JwtPayload validateAndParse(String token) throws JwtException {
    final var claims = extractAllClaims(token);

    return jwtPayloadFromClaims(claims);
  }

  private JwtPayload jwtPayloadFromClaims(Claims claims) {
    final var subject = claims.getSubject();
    final var expiration = claims.getExpiration();
    final var issuedAt = claims.getIssuedAt();
    final var notBefore = claims.getNotBefore();
    final var jwtId = claims.getId();
    final var audience = claims.getAudience();

    final var builder =
        JwtPayload.builder(Objects.requireNonNull(subject), expiration.toInstant().getEpochSecond())
            .issuedAt(issuedAt.toInstant().getEpochSecond());

    if (notBefore != null) {
      builder.notBefore(notBefore.toInstant().getEpochSecond());
    }

    if (jwtId != null) {
      builder.jwtId(jwtId);
    }

    if (audience != null) {
      builder.audiences(Objects.requireNonNull(List.of(audience.toArray(String[]::new))));
    }

    final var mappedClaims =
        claims.entrySet().stream()
            .filter(
                entry ->
                    !List.of(
                            Claims.SUBJECT,
                            Claims.EXPIRATION,
                            Claims.ISSUED_AT,
                            Claims.NOT_BEFORE,
                            Claims.ID,
                            Claims.AUDIENCE,
                            Claims.ISSUER)
                        .contains(entry.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    builder.claims(Objects.requireNonNull(mappedClaims));

    return builder.build();
  }

  private Claims extractAllClaims(String token) throws JwtException {
    try {

      return Objects.requireNonNull(
          Jwts.parser()
              .verifyWith(jwtSigningKey)
              .requireIssuer(jwtIssuer)
              .build()
              .parseSignedClaims(token)
              .getPayload());
    } catch (ExpiredJwtException e) {
      final var claims = e.getClaims();
      final var expiration = claims.getExpiration();

      logger.warn("JWT token has expired", e);

      throw new TokenExpiredException(
          "JWT token has expired", Objects.requireNonNull(expiration.toInstant()));
    } catch (
        @SuppressWarnings("squid:S2139")
        io.jsonwebtoken.JwtException e) {

      logger.error("Error validating JWT token", e);

      throw new JwtValidationException("Error validating JWT token", e);
    }
  }
}

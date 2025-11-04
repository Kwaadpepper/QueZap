package com.quezap.infrastructure.adapter.services;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.ports.services.ParticipationTokenService;
import com.quezap.infrastructure.adapter.services.config.ParticipationTokenConfig;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtService.JwtException;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload;
import com.quezap.lib.utils.UuidV7;

@Service
public class ParticipationTokenServiceImpl implements ParticipationTokenService {
  private final JwtService jwtService;
  private final int expirationSeconds;

  public ParticipationTokenServiceImpl(JwtService jwtService, ParticipationTokenConfig config) {
    this.jwtService = jwtService;
    this.expirationSeconds = config.expirationSeconds();
  }

  @Override
  public ParticipationToken generate(SessionId sessionId) {
    try {
      final var payload = from(sessionId);
      final var tokenString = jwtService.generateToken(payload);

      return new ParticipationToken(tokenString);
    } catch (JwtException e) {
      throw new ParticipationTokenGenerationException("Failed to generate participation token", e);
    }
  }

  @Override
  public SessionId validate(ParticipationToken participationToken) {
    try {
      final var payload = jwtService.validateAndParse(participationToken.value());
      final var subjectValue = Objects.requireNonNull(payload.sub(), "JWT subject is null");
      final var sessionIdValue =
          Objects.requireNonNull(
              UUID.fromString(subjectValue), "SessionId extracted from JWT is null");

      return new SessionId(sessionIdValue);
    } catch (JwtException e) {
      throw new ParticipationTokenGenerationException("Invalid participation token", e);
    }
  }

  private JwtPayload from(SessionId sessionId) {
    final var subject = sessionId.toString();
    final var now = System.currentTimeMillis();
    final var issuedAt = now / 1000;
    final var expiration = now / 1000 + expirationSeconds;
    final var notBefore = issuedAt;
    final var jwtId = UuidV7.randomUuid().toString();

    return JwtPayload.builder(subject, expiration)
        .issuedAt(issuedAt)
        .notBefore(notBefore)
        .jwtId(jwtId)
        .build();
  }

  public static class ParticipationTokenGenerationException extends RuntimeException {
    public ParticipationTokenGenerationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

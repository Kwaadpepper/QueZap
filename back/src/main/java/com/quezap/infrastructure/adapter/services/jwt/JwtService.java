package com.quezap.infrastructure.adapter.services.jwt;

import java.time.Instant;

import com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload;

public interface JwtService {

  String generateToken(JwtPayload payload) throws JwtException;

  JwtPayload validateAndParse(String token) throws JwtException;

  /** Base exception for JWT-related errors. */
  public static class JwtException extends Exception {
    public JwtException(String message) {
      super(message);
    }

    public JwtException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Exception thrown on a validation failure (signature, format, claims). */
  public static class JwtValidationException extends JwtException {
    public JwtValidationException(String message) {
      super(message);
    }

    public JwtValidationException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Specific exception for an expired token (based on 'exp' claim). */
  public static class TokenExpiredException extends JwtValidationException {
    private final Instant expiredAt;

    public TokenExpiredException(String message, Instant expiredAt) {
      super(message);
      this.expiredAt = expiredAt;
    }

    public Instant getExpiredAt() {
      return expiredAt;
    }
  }
}

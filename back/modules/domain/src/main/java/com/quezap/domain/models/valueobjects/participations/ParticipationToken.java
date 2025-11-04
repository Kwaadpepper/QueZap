package com.quezap.domain.models.valueobjects.participations;

import java.util.regex.Pattern;

import com.quezap.lib.utils.Domain;

public record ParticipationToken(String value) {

  public ParticipationToken {
    Domain.checkDomain(() -> !value.isBlank(), "Participation token cannot be blank");
    Domain.checkDomain(() -> isValidJwt(value), "Participation token must be a valid JWT format");
  }

  private Boolean isValidJwt(String token) {
    final var jwtRegex = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$";
    return Pattern.matches(jwtRegex, token);
  }
}

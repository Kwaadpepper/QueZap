package com.quezap.domain.models.valueobjects;

import java.util.UUID;

import com.quezap.lib.utils.Domain;

public record QuestionId(UUID value) {
  public QuestionId {
    Domain.checkDomain(() -> value.version() == 7, "A QuestionId must be a UUIDv7");
  }
}

package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.entities.EntityId;
import com.quezap.lib.utils.Domain;

public record QuestionId(UUID value) implements EntityId {
  public QuestionId {
    Domain.checkDomain(() -> value.version() == 7, "A QuestionId must be a UUIDv7");
  }

  public static QuestionId fromString(String value) {
    return new QuestionId(UUID.fromString(value));
  }
}

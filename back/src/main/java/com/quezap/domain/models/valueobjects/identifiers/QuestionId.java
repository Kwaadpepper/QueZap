package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.EntityId;
import com.quezap.lib.utils.Domain;

public record QuestionId(UUID value) implements EntityId {
  public QuestionId {
    Domain.checkDomain(() -> value.version() == 7, "A QuestionId must be a UUIDv7");
  }
}

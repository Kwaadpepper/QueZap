package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.entities.EntityId;
import com.quezap.lib.utils.Domain;

public record UserId(UUID value) implements EntityId {
  public UserId {
    Domain.checkDomain(() -> value.version() == 7, "A UserId must be a UUIDv7");
  }
}

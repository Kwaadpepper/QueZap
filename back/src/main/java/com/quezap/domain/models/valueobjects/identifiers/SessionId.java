package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.entities.EntityId;
import com.quezap.lib.utils.Domain;

public record SessionId(UUID value) implements EntityId {
  public SessionId {
    Domain.checkDomain(() -> value.version() == 7, "A SessionId must be a UUIDv7");
  }
}

package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.ddd.EntityId;
import com.quezap.lib.utils.Domain;

public record ThemeId(UUID value) implements EntityId {
  public ThemeId {
    Domain.checkDomain(() -> value.version() == 7, "A ThemeId must be a UUIDv7");
  }

  public static ThemeId fromString(String id) {
    final var uuid = UUID.fromString(id);

    return new ThemeId(uuid);
  }
}

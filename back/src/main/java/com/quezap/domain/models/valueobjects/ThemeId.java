package com.quezap.domain.models.valueobjects;

import java.util.UUID;

import com.quezap.lib.utils.Domain;

public record ThemeId(UUID value) {
  public ThemeId {
    Domain.checkDomain(() -> value.version() == 7, "A ThemeId must be a UUIDv7");
  }
}

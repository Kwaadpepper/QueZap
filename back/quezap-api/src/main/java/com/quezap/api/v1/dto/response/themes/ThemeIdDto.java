package com.quezap.api.v1.dto.response.themes;

import java.util.UUID;

public record ThemeIdDto(UUID id, UUID uuid) {
  public ThemeIdDto(UUID uuid) {
    this(uuid, uuid);
  }
}

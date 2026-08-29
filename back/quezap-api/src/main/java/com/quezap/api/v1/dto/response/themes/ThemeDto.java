package com.quezap.api.v1.dto.response.themes;

import java.util.UUID;

public record ThemeDto(UUID id, UUID uuid, String name) {
  public ThemeDto(UUID uuid, String name) {
    this(uuid, uuid, name);
  }
}

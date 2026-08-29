package com.quezap.api.v1.dto.response.questions;

import java.util.UUID;

public record QuestionShortInfoDto(UUID id, UUID uuid, String label) {
  public QuestionShortInfoDto(UUID uuid, String label) {
    this(uuid, uuid, label);
  }
}

package com.quezap.domain.models.valueobjects.identifiers;

import java.util.UUID;

import com.quezap.lib.utils.Domain;

public record CredentialId(UUID value) {
  public CredentialId {
    Domain.checkDomain(() -> value.version() == 7, "A CredentialId must be a UUIDv7");
  }
}

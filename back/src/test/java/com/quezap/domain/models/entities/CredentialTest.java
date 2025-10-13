package com.quezap.domain.models.entities;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CredentialTest {
  @Test
  void canInstantiate() {
    // GIVEN
    var hashedPassword = new HashedPassword("$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash");
    var hashedIdentifier = new HashedIdentifier("john.doe");

    // WHEN
    new Credential(hashedPassword, hashedIdentifier, null, ZonedDateTime.now(ZoneId.of("UTC")));

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateWithLastConnectionAt() {
    // GIVEN
    var hashedPassword = new HashedPassword("$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash");
    var hashedIdentifier = new HashedIdentifier("john.doe");
    var lastConnectionAt = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1);
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));

    // WHEN
    new Credential(hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var hashedPassword = new HashedPassword("$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash");
    var hashedIdentifier = new HashedIdentifier("john.doe");
    var lastConnectionAt = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1);
    var updatedAt = ZonedDateTime.now(ZoneId.of("UTC"));
    var id = UuidV7.randomUuid();

    // WHEN
    Credential.hydrate(id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

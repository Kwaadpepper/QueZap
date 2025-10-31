package com.quezap.domain.models.entities;

import java.time.temporal.ChronoUnit;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
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
    new Credential(hashedPassword, hashedIdentifier);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var hashedPassword = new HashedPassword("$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash");
    var hashedIdentifier = new HashedIdentifier("john.doe");
    var lastConnectionAt = TimelinePoint.now().minus(1, ChronoUnit.DAYS);
    var updatedAt = TimelinePoint.now();
    var id = UuidV7.randomUuid();

    // WHEN
    Credential.hydrate(id, hashedPassword, hashedIdentifier, lastConnectionAt, updatedAt);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }
}

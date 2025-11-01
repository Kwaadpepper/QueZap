package com.quezap.domain.models.entities;

import java.util.UUID;

import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserTest {
  @Test
  void canInstantiate() {
    // GIVEN
    var name = "some-name";
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));

    // WHEN
    new User(name, credentialId);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var name = "some-name";
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));
    var utc = TimelinePoint.now();
    var id = new UserId(UuidV7.randomUuid());

    // WHEN
    User.hydrate(id, name, credentialId, utc);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void cannotInstantiateWithBlankName() {
    // GIVEN
    var name = "   ";
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));

    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new User(name, credentialId));
  }

  @Test
  void cannotInstantiateWithNameTooLong() {
    // GIVEN
    var name = "A".repeat(256);
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6a-0000-000000000000"));

    // WHEN / THEN
    Assertions.assertThrows(IllegalDomainStateException.class, () -> new User(name, credentialId));
  }
}

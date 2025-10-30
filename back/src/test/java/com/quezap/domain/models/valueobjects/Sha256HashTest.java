package com.quezap.domain.models.valueobjects;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Sha256HashTest {

  @Test
  void canInstantiateSha256Hash() {
    // GIVEN
    byte[] validHash = new byte[32];

    // WHEN
    new Sha256Hash(validHash);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canCompareSha256Hashes() {
    // GIVEN
    byte[] hashBytes1 = new byte[32];
    byte[] hashBytes2 = new byte[32];
    for (int i = 0; i < 32; i++) {
      hashBytes1[i] = (byte) i;
      hashBytes2[i] = (byte) i;
    }

    var hash1 = new Sha256Hash(hashBytes1);
    var hash2 = new Sha256Hash(hashBytes2);

    // WHEN / THEN
    Assertions.assertEquals(hash1, hash2);
  }

  @Test
  void cannotInstantiateSha256HashWithInvalidLength() {
    // GIVEN
    byte[] invalidHash = new byte[16];

    // WHEN / THEN
    Assertions.assertThrows(
        IllegalDomainStateException.class,
        () -> {
          new Sha256Hash(invalidHash);
        });
  }
}

package com.quezap.infrastructure.adapter.services;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.port.services.IdentifierHasher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdentifierHasherTest {
  private final IdentifierHasher identifierHasher;

  public IdentifierHasherTest() {
    this.identifierHasher = new IdentifierHasherImpl();
  }

  @Test
  void canHashIdentifier() {
    // GIVEN
    var identifier = new RawIdentifier("john.doe");

    // WHEN
    identifierHasher.hash(identifier);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canVerifiyThatDifferentValuesHashesAreNotEquals() {
    // GIVEN
    var identifier1 = new RawIdentifier("rawiId1");
    var identifier2 = new RawIdentifier("rawId2");

    // WHEN
    var output1 = identifierHasher.hash(identifier1);
    var output2 = identifierHasher.hash(identifier2);

    // THEN
    Assertions.assertNotEquals(output1, output2);
  }

  @Test
  void canVerifiyThatSameValueHashedMultipleTimesProducesIdenticalResult() {
    // GIVEN
    var identifier = new RawIdentifier("rawiId");

    // WHEN
    var output1 = identifierHasher.hash(identifier);
    var output2 = identifierHasher.hash(identifier);

    // THEN
    Assertions.assertEquals(output1, output2);
  }
}

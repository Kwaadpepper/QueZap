package com.quezap.infrastructure.adapter.services;

import org.springframework.mock.env.MockEnvironment;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.infrastructure.adapter.services.config.IdentifierHasherConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class IdentifierHasherTest {
  private final IdentifierHasher identifierHasher;

  public IdentifierHasherTest() {
    this.identifierHasher =
        new IdentifierHasherImpl(
            new IdentifierHasherConfig("test-pepper-at-least-32-bytes-long-for-hmac-sha256"));
  }

  @Test
  void canHashIdentifierWithHmacSha256Hex() {
    final var identifier = new RawIdentifier("john.doe");

    final var hashed = identifierHasher.hash(identifier);

    Assertions.assertThat(hashed.value()).hasSize(64).matches("^[0-9a-f]{64}$");
  }

  @Test
  void canVerifyThatDifferentValuesHashesAreNotEqual() {
    final var identifier1 = new RawIdentifier("rawId1");
    final var identifier2 = new RawIdentifier("rawId2");

    final var output1 = identifierHasher.hash(identifier1);
    final var output2 = identifierHasher.hash(identifier2);

    Assertions.assertThat(output1).isNotEqualTo(output2);
  }

  @Test
  void canVerifyThatSameValueHashedMultipleTimesProducesIdenticalResult() {
    final var identifier = new RawIdentifier("rawId");

    final var output1 = identifierHasher.hash(identifier);
    final var output2 = identifierHasher.hash(identifier);

    Assertions.assertThat(output1).isEqualTo(output2);
  }

  @Test
  void producesDifferentHashesForDifferentPeppers() {
    final var otherHasher =
        new IdentifierHasherImpl(
            new IdentifierHasherConfig("another-pepper-at-least-32-bytes-long-for-hmac"));
    final var identifier = new RawIdentifier("alice@example.com");

    Assertions.assertThat(identifierHasher.hash(identifier))
        .isNotEqualTo(otherHasher.hash(identifier));
  }

  @Test
  void rejectsShortPepper() {
    Assertions.assertThatThrownBy(() -> new IdentifierHasherConfig("too-short"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  @Test
  void rejectsDefaultDevPepperInProdProfile() {
    final var prodEnv = new MockEnvironment();
    prodEnv.setActiveProfiles("prod");

    Assertions.assertThatThrownBy(
            () -> new IdentifierHasherConfig(IdentifierHasherConfig.DEFAULT_DEV_PEPPER, prodEnv))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Default development identifier pepper");
  }
}

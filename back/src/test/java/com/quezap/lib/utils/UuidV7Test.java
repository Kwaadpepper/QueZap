package com.quezap.lib.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UuidV7Test {
  @Test
  void canGenerateUuidV7() {
    // WHEN
    UuidV7.randomUuid();

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canExtractTimestamp() {
    // GIVEN
    var begin = System.currentTimeMillis();
    var uuid = UuidV7.randomUuid();

    // WHEN
    var instant = UuidV7.extractInstant(uuid);

    // THEN
    var end = System.currentTimeMillis();
    Assertions.assertTrue(instant.toEpochMilli() >= begin && instant.toEpochMilli() <= end);
  }
}

package com.quezap.infrastructure.adapter.services;

import com.quezap.application.config.SessionCodeConfig;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.ports.services.SessionCodeEncoder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SessionCodeEncoderTest {
  private static final int LENGTH = 6;
  private static final long SEED = 23;
  private static final char[] dict = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789".toCharArray();

  private final SessionCodeEncoder sessionCodeGenerator;

  SessionCodeEncoderTest() {
    final var config = new SessionCodeConfig(LENGTH, SEED, new String(dict));
    this.sessionCodeGenerator = new SessionCodeEncoderImpl(config);
  }

  @Test
  void canGenerateSessionCode() {
    // GIVEN
    var sessionNumber = new SessionNumber(1);

    // WHEN
    sessionCodeGenerator.encode(sessionNumber);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canRetriveSessionNumber() {
    // GIVEN
    var sessionCode = new SessionCode("QC2G29");
    var sessionNumberValue = 1123450;

    // WHEN
    var sessionNumber = sessionCodeGenerator.decode(sessionCode);

    // THEN
    Assertions.assertEquals(sessionNumberValue, sessionNumber.value());
  }
}

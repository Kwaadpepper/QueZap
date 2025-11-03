package com.quezap.infrastructure.adapter.services.jwt;

import com.quezap.infrastructure.adapter.services.config.JwtServiceConfig;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private final JwtService jwtService;

  JwtServiceTest() {
    final var config =
        new JwtServiceConfig(
            "443a00f5072a066891ced7ea96f1926a1bcdb9f6d8ad4bbe3722cb59a2e15682", "example.net");
    this.jwtService = new JwtServiceImpl(config);
  }

  @Test
  void canGenerateJwtToken() throws JwtService.JwtException {
    // GIVEN
    var jwtPayload = JwtPayload.builder("subject", 1716239022L).issuedAt(1716239000L).build();

    // WHEN
    final var output = jwtService.generateToken(jwtPayload);

    // THEN
    Assertions.assertThat(output).isNotBlank();
  }

  @Test
  void canDecodeJwtToken() throws JwtService.JwtException {
    // GIVEN
    var expiration = System.currentTimeMillis() / 1000 + 3600;
    var jwtPayload =
        JwtPayload.builder("subject", expiration)
            .audience("example-audience")
            .jwtId("unique-jwt-id-12345")
            .issuedAt(System.currentTimeMillis() / 1000)
            .claim("role", "admin")
            .notBefore(System.currentTimeMillis() / 1000 - 60)
            .build();

    // WHEN
    var token = jwtService.generateToken(jwtPayload);
    var output = jwtService.validateAndParse(token);

    // THEN
    Assertions.assertThat(output).isEqualTo(jwtPayload);
  }
}

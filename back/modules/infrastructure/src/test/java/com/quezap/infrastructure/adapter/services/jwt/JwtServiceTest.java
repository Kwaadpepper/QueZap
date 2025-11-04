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
            "1c0d219a2ea573bab0b8e1f01c1594614209a92c0df567c00a1"
                + "131c4088cd3afd91fee49bba6d2ae4333a97d0b35e0149e9d5cc9461e1a28d1c38ab931e80a61",
            "example.net");
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

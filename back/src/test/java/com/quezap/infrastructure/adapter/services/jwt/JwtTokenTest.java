package com.quezap.infrastructure.adapter.services.jwt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JwtTokenTest {

  @Test
  void canInstanciateJwtContent() {
    // GIVEN
    var sub = "subject";
    var aud = List.of("audience1", "audience2");
    var exp = 1716239022L;
    var iat = 1616239022L;
    Long nbf = null;
    String jti = null;
    Map<String, Object> customClaims = Map.of("role", "admin", "scope", "read:write");

    // WHEN
    new JwtToken.JwtPayload(sub, aud, exp, iat, nbf, jti, customClaims);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canUseJwtPayloadBuilder() {
    // GIVEN
    var sub = "subject";
    var aud = List.of("audience1", "audience2");
    var exp = 1716239022L;
    var iat = 1616239022L;
    Long nbf = null;
    String jti = UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString();
    Map<String, Object> customClaims = Map.of("role", "admin", "scope", "read:write");
    var builder = JwtToken.JwtPayload.builder(sub, exp);

    // WHEN
    var payload =
        builder.audiences(aud).issuedAt(iat).notBefore(nbf).jwtId(jti).claims(customClaims).build();

    // THEN
    Assertions.assertThat(payload)
        .extracting(
            JwtToken.JwtPayload::sub,
            JwtToken.JwtPayload::aud,
            JwtToken.JwtPayload::exp,
            JwtToken.JwtPayload::iat,
            JwtToken.JwtPayload::nbf,
            JwtToken.JwtPayload::jti,
            JwtToken.JwtPayload::customClaims)
        .containsExactly(sub, aud, exp, iat, nbf, jti, customClaims);
  }
}

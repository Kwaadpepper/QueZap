package com.quezap.infrastructure.adapter.services;

import java.util.UUID;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.infrastructure.adapter.services.config.ParticipationTokenConfig;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtService.JwtException;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ParticipationTokenServiceTest {
  private final ParticipationTokenServiceImpl participationTokenService;
  private final JwtService jwtService;

  public ParticipationTokenServiceTest() {
    final var config = new ParticipationTokenConfig(3600);
    this.jwtService = MockEntity.mock(JwtService.class);
    this.participationTokenService = new ParticipationTokenServiceImpl(jwtService, config);
  }

  @Test
  void canGenerateParticipationToken() throws JwtException {
    // GIVEN
    var sessionIdValue = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var sessionId = new SessionId(sessionIdValue);
    var jwtToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9."
            + "eyJpc3MiOiJleGFtcGxlLm5ldCIsInN1YiI6InN1YmplY3QiLCJleHAiOjE3MTYyM"
            + "zkwMjIsImlhdCI6MTcxNjIzOTAwMCwianRpIjoiYTI1NTUxOGMtYjBlZS00MDg1LTkz"
            + "MjYtNjQ0N2UwNDZkYTlmIn0.kbrxCXQt9ptSu6vztRiOwLUwbwtLkewSqnnrHVx7ws6XW"
            + "__mxNDNLIIfbvv9uZFp5oRuDYsGboE9fwyzotustg";

    // WHEN
    Mockito.when(jwtService.generateToken(MockEntity.any(JwtPayload.class))).thenReturn(jwtToken);
    participationTokenService.generate(sessionId);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canValidateParticipationToken() throws JwtException {
    // GIVEN
    var sessionIdValue = UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000");
    var sessionId = new SessionId(sessionIdValue);
    var expiration = System.currentTimeMillis() + 3600_000;
    var jwtPayload = JwtPayload.builder(sessionIdValue.toString(), expiration).build();
    var jwtToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9."
            + "eyJpc3MiOiJleGFtcGxlLm5ldCIsInN1YiI6InN1YmplY3QiLCJleHAiOjE3MTYyM"
            + "zkwMjIsImlhdCI6MTcxNjIzOTAwMCwianRpIjoiYTI1NTUxOGMtYjBlZS00MDg1LTkz"
            + "MjYtNjQ0N2UwNDZkYTlmIn0.kbrxCXQt9ptSu6vztRiOwLUwbwtLkewSqnnrHVx7ws6XW"
            + "__mxNDNLIIfbvv9uZFp5oRuDYsGboE9fwyzotustg";

    // WHEN
    Mockito.when(jwtService.generateToken(MockEntity.any(JwtPayload.class))).thenReturn(jwtToken);
    Mockito.when(jwtService.validateAndParse(jwtToken)).thenReturn(jwtPayload);
    var participationToken = participationTokenService.generate(sessionId);
    var validatedSessionId = participationTokenService.validate(participationToken);

    // THEN
    Assertions.assertThat(validatedSessionId).isEqualTo(sessionId);
  }
}

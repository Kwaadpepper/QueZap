package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface ParticipateSession {
  record Input(ParticipantName name, SessionCode code) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record Participation(ParticipationToken token) implements Output {}
  }

  public interface ParticipateSessionUseCase
      extends UseCaseHandler<ParticipateSession.Input, ParticipateSession.Output> {}
}

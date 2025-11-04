package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface ParticipateSession {
  record Input(ParticipantName name, SessionCode code) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record Participation(ParticipationToken token) implements Output {}
  }

  public interface ParticipateSessionUsecase
      extends UsecaseHandler<ParticipateSession.Input, ParticipateSession.Output> {}
}

package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface StartSession {
  record Input(SessionId id) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record Started() implements Output {}
  }

  public interface StartSessionUsecase
      extends UsecaseHandler<StartSession.Input, StartSession.Output> {}
}

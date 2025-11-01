package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface StartSession {
  record Input(SessionId id) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record Started() implements Output {}
  }

  public interface StartSessionUseCase
      extends UseCaseHandler<StartSession.Input, StartSession.Output> {}
}

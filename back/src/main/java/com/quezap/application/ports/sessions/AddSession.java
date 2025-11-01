package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface AddSession {
  record Input(SessionName name, UserId user) implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record SessionAdded(SessionId id) implements Output {}
  }

  public interface AddSessionUseCase extends UseCaseHandler<AddSession.Input, AddSession.Output> {}
}

package com.quezap.application.ports.sessions;

import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AddSession {
  record Input(SessionName name, UserId user) implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record SessionAdded(SessionId id) implements Output {}
  }

  public interface AddSessionUsecase extends UsecaseHandler<AddSession.Input, AddSession.Output> {}
}

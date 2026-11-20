package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface JoinSession {
  record Input(String code, String participantName, @Nullable String existingToken)
      implements UsecaseInput {}

  record Output(UUID sessionId, String participationToken, String participantName)
      implements UsecaseOutput {}

  interface JoinSessionUsecase extends UsecaseHandler<Input, Output> {}
}

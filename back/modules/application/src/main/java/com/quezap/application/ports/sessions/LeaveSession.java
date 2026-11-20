package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface LeaveSession {
  record Input(UUID sessionId, @Nullable String participationToken) implements UsecaseInput {}

  record Output() implements UsecaseOutput {}

  interface LeaveSessionUsecase extends UsecaseHandler<Input, Output> {}
}

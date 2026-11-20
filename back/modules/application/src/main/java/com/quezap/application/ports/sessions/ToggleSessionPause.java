package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface ToggleSessionPause {
  record Input(UUID sessionId, UUID requesterId, boolean paused) implements UsecaseInput {}

  record Output(UUID sessionId, boolean isPaused) implements UsecaseOutput {}

  interface ToggleSessionPauseUsecase extends UsecaseHandler<Input, Output> {}
}

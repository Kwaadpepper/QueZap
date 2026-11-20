package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RevealSessionQuestion {
  record Input(UUID sessionId, UUID requesterId) implements UsecaseInput {}

  record Output() implements UsecaseOutput {}

  interface RevealSessionQuestionUsecase extends UsecaseHandler<Input, Output> {}
}

package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface CreateSession {
  record Input(UUID authorId, String name, @Nullable UUID quezapId) implements UsecaseInput {}

  record Output(UUID id, long number, String code) implements UsecaseOutput {}

  interface CreateSessionUsecase extends UsecaseHandler<Input, Output> {}
}

package com.quezap.application.ports.sessions;

import java.time.Instant;
import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface SubmitSessionAnswer {
  record Input(
      UUID sessionId,
      @Nullable String participationToken,
      @Nullable Integer slideIndex,
      @Nullable Integer answerIndex,
      @Nullable String textAnswer)
      implements UsecaseInput {}

  record Output(boolean accepted, Instant receivedAt) implements UsecaseOutput {}

  interface SubmitSessionAnswerUsecase extends UsecaseHandler<Input, Output> {}
}

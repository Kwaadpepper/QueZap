package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface UpdateSessionSettings {
  record Input(
      UUID sessionId,
      UUID requesterId,
      @Nullable String progressionMode,
      @Nullable Integer explanationDurationSeconds,
      @Nullable Boolean mobileCorrectionEnabled)
      implements UsecaseInput {}

  record Output(
      String progressionMode, int explanationDurationSeconds, boolean mobileCorrectionEnabled)
      implements UsecaseOutput {}

  interface UpdateSessionSettingsUsecase extends UsecaseHandler<Input, Output> {}
}

package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface KickSessionParticipant {
  record Input(UUID sessionId, UUID requesterId, String participantName) implements UsecaseInput {}

  record Output() implements UsecaseOutput {}

  interface KickSessionParticipantUsecase extends UsecaseHandler<Input, Output> {}
}

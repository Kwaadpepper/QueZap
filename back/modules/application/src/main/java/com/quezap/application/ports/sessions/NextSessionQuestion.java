package com.quezap.application.ports.sessions;

import java.util.UUID;

import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface NextSessionQuestion {
  record Input(UUID sessionId, UUID requesterId) implements UsecaseInput {}

  record Output(int currentSlideIndex, int totalSlides) implements UsecaseOutput {}

  interface NextSessionQuestionUsecase extends UsecaseHandler<Input, Output> {}
}

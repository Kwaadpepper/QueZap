package com.quezap.lib.ddd;

import com.quezap.lib.ddd.events.DomainEvent;

import org.jspecify.annotations.Nullable;

public interface UseCaseExecutor {
  <I extends UseCaseInput, O extends UseCaseOutput> @Nullable O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput);

  void publish(DomainEvent<?> event);
}

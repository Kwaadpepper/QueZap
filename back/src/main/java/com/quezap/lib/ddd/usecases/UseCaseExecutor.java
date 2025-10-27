package com.quezap.lib.ddd.usecases;

import com.quezap.lib.ddd.events.DomainEvent;

public interface UseCaseExecutor {
  <I extends UseCaseInput, O extends UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput);

  void publish(DomainEvent<?> event);
}

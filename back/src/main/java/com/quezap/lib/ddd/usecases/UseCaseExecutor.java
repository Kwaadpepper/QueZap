package com.quezap.lib.ddd.usecases;

public interface UseCaseExecutor {
  <I extends UseCaseInput, O extends UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput);
}

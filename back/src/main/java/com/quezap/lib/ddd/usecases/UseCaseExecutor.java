package com.quezap.lib.ddd.usecases;

import org.jspecify.annotations.Nullable;

public interface UseCaseExecutor {
  <I extends UseCaseInput, O extends UseCaseOutput> @Nullable O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput);
}

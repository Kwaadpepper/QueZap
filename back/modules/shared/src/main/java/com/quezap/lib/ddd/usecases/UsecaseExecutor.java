package com.quezap.lib.ddd.usecases;

import org.jspecify.annotations.NonNull;

public interface UsecaseExecutor {
  <I extends UsecaseInput, O extends UsecaseOutput> O execute(
      UsecaseHandler<I, O> useCaseHandler, @NonNull I usecaseInput);
}

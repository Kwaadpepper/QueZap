package com.quezap.lib.ddd.usecases;

public interface UseCaseHandler<I extends UseCaseInput, O extends UseCaseOutput> {
  /**
   * Executes the use case with the provided input.
   *
   * @param usecaseInput the input for the use case
   * @param unitOfWork Unit of work events for the current execution.
   * @return the output of the use case
   */
  O handle(I usecaseInput, UnitOfWorkEvents unitOfWork);
}

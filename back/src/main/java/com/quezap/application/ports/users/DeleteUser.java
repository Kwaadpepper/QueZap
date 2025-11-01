package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface DeleteUser {
  sealed interface Input extends UseCaseInput {
    record UserName(String name) implements Input {}

    record Id(UserId id) implements Input {}
  }

  sealed interface Output extends UseCaseOutput {
    record UserDeleted() implements Output {}
  }

  public interface DeleteUserUseCase extends UseCaseHandler<DeleteUser.Input, DeleteUser.Output> {}
}

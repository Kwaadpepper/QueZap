package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface DeleteUser {
  sealed interface Input extends UsecaseInput {
    record UserName(String name) implements Input {}

    record Id(UserId id) implements Input {}
  }

  sealed interface Output extends UsecaseOutput {
    record UserDeleted() implements Output {}
  }

  public interface DeleteUserUsecase extends UsecaseHandler<DeleteUser.Input, DeleteUser.Output> {}
}

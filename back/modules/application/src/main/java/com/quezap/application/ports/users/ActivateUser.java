package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface ActivateUser {
  sealed interface Input extends UsecaseInput {
    record Id(UserId id) implements Input {}

    record UserName(String name) implements Input {}
  }

  sealed interface Output extends UsecaseOutput {
    record UserActivated(UserId id) implements Output {}
  }

  public interface ActivateUserUsecase
      extends UsecaseHandler<ActivateUser.Input, ActivateUser.Output> {}
}

package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface UpdateUserPassword {
  sealed interface Input extends UseCaseInput {
    record UserName(String name, RawPassword newPassword) implements Input {}

    record Id(UserId id, RawPassword newPassword) implements Input {}
  }

  sealed interface Output extends UseCaseOutput {
    record PasswordUpdated() implements Output {}
  }

  public interface UpdateUserPasswordUseCase
      extends UseCaseHandler<UpdateUserPassword.Input, UpdateUserPassword.Output> {}
}

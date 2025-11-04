package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface UpdateUserPassword {
  sealed interface Input extends UsecaseInput {
    record UserName(String name, RawPassword newPassword) implements Input {}

    record Id(UserId id, RawPassword newPassword) implements Input {}
  }

  sealed interface Output extends UsecaseOutput {
    record PasswordUpdated() implements Output {}
  }

  public interface UpdateUserPasswordUsecase
      extends UsecaseHandler<UpdateUserPassword.Input, UpdateUserPassword.Output> {}
}

package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public interface AddUser {
  record Input(String name, RawIdentifier identifier, RawPassword password)
      implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record UserAdded(UserId id) implements Output {}
  }

  public interface AddUserUseCase extends UseCaseHandler<AddUser.Input, AddUser.Output> {}
}

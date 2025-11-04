package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AddUser {
  record Input(String name, RawIdentifier identifier, RawPassword password)
      implements UsecaseInput {}

  sealed interface Output extends UsecaseOutput {
    record UserAdded(UserId id) implements Output {}
  }

  public interface AddUserUsecase extends UsecaseHandler<AddUser.Input, AddUser.Output> {}
}

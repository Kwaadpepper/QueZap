package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface RenameUser {
  sealed interface Input extends UsecaseInput {
    record Id(UserId id, String newName) implements Input {}

    record UserName(String currentName, String newName) implements Input {}
  }

  sealed interface Output extends UsecaseOutput {
    record UserRenamed(UserId id, String newName) implements Output {}
  }

  public interface RenameUserUsecase extends UsecaseHandler<RenameUser.Input, RenameUser.Output> {}
}

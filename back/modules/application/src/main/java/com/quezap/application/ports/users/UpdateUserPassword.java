package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

import org.jspecify.annotations.Nullable;

public interface UpdateUserPassword {
  sealed interface Input extends UsecaseInput {
    record UserName(String name, @Nullable PasswordCandidate oldPassword, RawPassword newPassword)
        implements Input {
      public UserName(String name, RawPassword newPassword) {
        this(name, null, newPassword);
      }
    }

    record Id(UserId id, @Nullable PasswordCandidate oldPassword, RawPassword newPassword)
        implements Input {
      public Id(UserId id, RawPassword newPassword) {
        this(id, null, newPassword);
      }
    }
  }

  sealed interface Output extends UsecaseOutput {
    record PasswordUpdated() implements Output {}
  }

  public interface UpdateUserPasswordUsecase
      extends UsecaseHandler<UpdateUserPassword.Input, UpdateUserPassword.Output> {}
}

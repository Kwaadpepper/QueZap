package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;

public interface AuthenticateUser {
  record Input(RawIdentifier identifier, PasswordCandidate password) implements UsecaseInput {}

  record Output(UserId id, String name) implements UsecaseOutput {}

  public interface AuthenticateUserUsecase
      extends UsecaseHandler<AuthenticateUser.Input, AuthenticateUser.Output> {}
}

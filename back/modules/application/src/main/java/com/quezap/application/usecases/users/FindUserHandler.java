package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.FindUserError;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.application.ports.users.FindUser.Input;
import com.quezap.application.ports.users.FindUser.Output;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class FindUserHandler implements FindUserUsecase {
  private final UserRepository userRepository;

  public FindUserHandler(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var user =
        userRepository
            .find(input.id())
            .orElseThrow(ApplicationConstraintException.with(FindUserError.NO_SUCH_USER));

    return new Output(user.getId(), user.getName(), user.getCreatedAt(), user.getUpdatedAt());
  }
}

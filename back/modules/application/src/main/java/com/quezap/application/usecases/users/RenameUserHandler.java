package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.RenameUserError;
import com.quezap.application.ports.users.RenameUser.Input;
import com.quezap.application.ports.users.RenameUser.Output;
import com.quezap.application.ports.users.RenameUser.RenameUserUsecase;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class RenameUserHandler implements RenameUserUsecase {
  private final UserRepository userRepository;

  public RenameUserHandler(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final User user = findTargetUser(input);
    final String newName = extractNewName(input);

    if (!user.getName().equals(newName)) {
      userRepository
          .findByName(newName)
          .filter(existing -> !existing.getId().equals(user.getId()))
          .ifPresent(
              existing -> {
                throw new ApplicationConstraintException(RenameUserError.USER_NAME_ALREADY_TAKEN);
              });
    }

    user.rename(newName);
    userRepository.persist(user);

    return new Output.UserRenamed(user.getId(), user.getName());
  }

  private User findTargetUser(Input input) {
    return switch (input) {
      case Input.Id(var id, var ignored) ->
          userRepository
              .find(id)
              .orElseThrow(ApplicationConstraintException.with(RenameUserError.NO_SUCH_USER));
      case Input.UserName(var currentName, var ignored) ->
          userRepository
              .findByName(currentName)
              .orElseThrow(ApplicationConstraintException.with(RenameUserError.NO_SUCH_USER));
    };
  }

  private String extractNewName(Input input) {
    return switch (input) {
      case Input.Id(var ignored, var newName) -> newName;
      case Input.UserName(var ignored, var newName) -> newName;
    };
  }
}

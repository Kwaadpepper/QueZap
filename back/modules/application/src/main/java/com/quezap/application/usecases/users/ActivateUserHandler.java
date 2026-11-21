package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.ActivateUserError;
import com.quezap.application.ports.users.ActivateUser.ActivateUserUsecase;
import com.quezap.application.ports.users.ActivateUser.Input;
import com.quezap.application.ports.users.ActivateUser.Output;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class ActivateUserHandler implements ActivateUserUsecase {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;

  public ActivateUserHandler(
      UserRepository userRepository, CredentialRepository credentialRepository) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final User user = findTargetUser(input);

    final var credential =
        credentialRepository
            .find(user.getCredential())
            .orElseThrow(() -> new IllegalDomainStateException("A user is missing credential"));

    if (!credential.isActivated()) {
      credential.activate();
      credentialRepository.persist(credential);
    }

    return new Output.UserActivated(user.getId());
  }

  private User findTargetUser(Input input) {
    return switch (input) {
      case Input.Id(var id) ->
          userRepository
              .find(id)
              .orElseThrow(ApplicationConstraintException.with(ActivateUserError.NO_SUCH_USER));
      case Input.UserName(var name) ->
          userRepository
              .findByName(name)
              .orElseThrow(ApplicationConstraintException.with(ActivateUserError.NO_SUCH_USER));
    };
  }
}

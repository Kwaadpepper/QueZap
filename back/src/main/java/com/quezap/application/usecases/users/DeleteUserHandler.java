package com.quezap.application.usecases.users;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.DeleteUser.Input;
import com.quezap.application.ports.users.DeleteUser.Output;
import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Usecase
final class DeleteUserHandler implements DeleteUserUsecase {
  private static final Logger logger = LoggerFactory.getLogger(DeleteUserHandler.class);

  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;

  public DeleteUserHandler(
      UserRepository userRepository, CredentialRepository credentialRepository) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var userId = fromInput(input);

    userRepository
        .find(userId)
        .ifPresentOrElse(
            this::deleteCredentialAndUser,
            DomainConstraintException.throwWith(DeleteUserError.NO_SUCH_USER));

    return new Output.UserDeleted();
  }

  private UserId fromInput(Input input) {
    return switch (input) {
      case Input.Id(var id) -> id;
      case Input.UserName(var name) ->
          userRepository
              .findByName(name)
              .<UserId>map(User::getId)
              .orElseThrow(DomainConstraintException.with(DeleteUserError.NO_SUCH_USER));
    };
  }

  private void deleteCredentialAndUser(User user) {
    final var credentialId = user.getCredential();

    credentialRepository
        .find(credentialId)
        .ifPresentOrElse(
            credentialRepository::delete,
            () ->
                logger.warn(
                    "Credential {} not found when deleting user {}",
                    credentialId.value(),
                    user.getId().value()));

    userRepository.delete(user);
  }
}

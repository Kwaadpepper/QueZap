package com.quezap.application.usecases.users;

import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public sealed interface DeleteUser {

  public sealed interface Input extends UseCaseInput {
    record UserName(String name) implements Input {}

    record Id(UserId id) implements Input {}
  }

  sealed interface Output extends UseCaseOutput {
    record UserDeleted() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, DeleteUser {
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public Handler(UserRepository userRepository, CredentialRepository credentialRepository) {
      this.userRepository = userRepository;
      this.credentialRepository = credentialRepository;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      final var userId = fromInput(usecaseInput);

      userRepository
          .find(userId)
          .ifPresentOrElse(
              this::deleteCredentialAndUser,
              DomainConstraintException.throwWith(DeleteUserError.NO_SUCH_USER));

      return new Output.UserDeleted();
    }

    private UserId fromInput(Input usecaseInput) {
      return switch (usecaseInput) {
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
}

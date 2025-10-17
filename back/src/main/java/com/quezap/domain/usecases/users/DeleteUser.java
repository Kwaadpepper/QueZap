package com.quezap.domain.usecases.users;

import java.util.Optional;

import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

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
    public Output handle(Input usecaseInput) {
      final var userNotFoundException = new DomainConstraintException(DeleteUserError.NO_SUCH_USER);
      final UserId userId =
          switch (usecaseInput) {
            case Input.Id(UserId id) -> id;
            case Input.UserName(String name) ->
                Optional.ofNullable(userRepository.findByName(name))
                    .map(AggregateRoot::getId)
                    .map(UserId::new)
                    .orElseThrow(() -> userNotFoundException);
          };
      final var user = userRepository.find(userId.value());

      if (user == null) {
        throw userNotFoundException;
      }

      final var credential = credentialRepository.find(user.getCredential().value());

      if (credential == null) {
        logger.warn("User {} has no associated credential, removing user anyway.", userId.value());
      } else {
        credentialRepository.delete(credential);
      }

      userRepository.delete(user);

      return new Output.UserDeleted();
    }
  }
}

package com.quezap.domain.usecases.users;

import java.util.Optional;

import com.quezap.domain.errors.users.UpdateUserPasswordError;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface UpdateUserPassword {
  sealed interface Input extends UseCaseInput {
    record UserName(String name, RawPassword newPassword) implements Input {}

    record Id(UserId id, RawPassword newPassword) implements Input {}
  }

  sealed interface Output extends UseCaseOutput {
    record PasswordUpdated() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, UpdateUserPassword {
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordHasher passwordHasher;

    public Handler(
        UserRepository userRepository,
        CredentialRepository credentialRepository,
        PasswordHasher passwordHasher) {
      this.userRepository = userRepository;
      this.credentialRepository = credentialRepository;
      this.passwordHasher = passwordHasher;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var userNotFoundException =
          new DomainConstraintException(UpdateUserPasswordError.NO_SUCH_USER);
      final UserId userId =
          switch (usecaseInput) {
            case Input.Id(UserId id, RawPassword pwd) -> id;
            case Input.UserName(String name, RawPassword pwd) ->
                Optional.ofNullable(userRepository.findByName(name))
                    .map(AggregateRoot::getId)
                    .map(UserId::new)
                    .orElseThrow(() -> userNotFoundException);
          };
      final var newPassword =
          switch (usecaseInput) {
            case Input.Id(UserId id, RawPassword pwd) -> pwd;
            case Input.UserName(String name, RawPassword pwd) -> pwd;
          };
      final var user = userRepository.find(userId.value());

      if (user == null) {
        throw userNotFoundException;
      }

      final var credential =
          Optional.ofNullable(credentialRepository.find(user.getCredential().value()))
              .orElseThrow(() -> new IllegalDomainStateException("A user is missing credential"));
      final var hashedNewPassword = passwordHasher.hash(newPassword);

      credential.updatePassword(hashedNewPassword);
      credentialRepository.save(credential);

      return new Output.PasswordUpdated();
    }
  }
}

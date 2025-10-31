package com.quezap.domain.usecases.users;

import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.errors.users.UpdateUserPasswordError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
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
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      final UserId userId = getUserIdFromInput(usecaseInput);
      final var newPassword = getPasswordFromInput(usecaseInput);

      userRepository
          .find(userId)
          .ifPresentOrElse(
              user -> updateCredentialOf(user, newPassword),
              DomainConstraintException.throwWith(DeleteUserError.NO_SUCH_USER));

      return new Output.PasswordUpdated();
    }

    private UserId getUserIdFromInput(Input usecaseInput) {
      return switch (usecaseInput) {
        case Input.Id input -> input.id();
        case Input.UserName input ->
            userRepository
                .findByName(input.name())
                .<UserId>map(User::getId)
                .orElseThrow(DomainConstraintException.with(UpdateUserPasswordError.NO_SUCH_USER));
      };
    }

    private RawPassword getPasswordFromInput(Input usecaseInput) {
      return switch (usecaseInput) {
        case Input.Id input -> input.newPassword();
        case Input.UserName input -> input.newPassword();
      };
    }

    private void updateCredentialOf(User user, RawPassword newPassword) {
      credentialRepository
          .find(user.getCredential())
          .ifPresentOrElse(
              cred -> updateAndPersistCredential(cred, newPassword),
              IllegalDomainStateException.throwWith("A user is missing credential"));
    }

    private void updateAndPersistCredential(Credential credential, RawPassword newPassword) {
      final var hashedNewPassword = passwordHasher.hash(newPassword);

      credential.updatePassword(hashedNewPassword);
      credentialRepository.persist(credential);
    }
  }
}

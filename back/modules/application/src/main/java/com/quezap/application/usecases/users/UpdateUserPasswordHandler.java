package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.DeleteUserError;
import com.quezap.application.exceptions.users.UpdateUserPasswordError;
import com.quezap.application.ports.users.UpdateUserPassword.Input;
import com.quezap.application.ports.users.UpdateUserPassword.Output;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

import org.jspecify.annotations.Nullable;

@Usecase
final class UpdateUserPasswordHandler implements UpdateUserPasswordUsecase {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final PasswordHasher passwordHasher;

  public UpdateUserPasswordHandler(
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final UserId userId = getUserIdFromInput(input);
    final var oldPassword = getOldPasswordFromInput(input);
    final var newPassword = getPasswordFromInput(input);

    userRepository
        .find(userId)
        .ifPresentOrElse(
            user -> updateCredentialOf(user, oldPassword, newPassword),
            ApplicationConstraintException.throwWith(DeleteUserError.NO_SUCH_USER));

    return new Output.PasswordUpdated();
  }

  private UserId getUserIdFromInput(Input input) {
    return switch (input) {
      case Input.Id inputId -> inputId.id();
      case Input.UserName inputName ->
          userRepository
              .findByName(inputName.name())
              .<UserId>map(User::getId)
              .orElseThrow(
                  ApplicationConstraintException.with(UpdateUserPasswordError.NO_SUCH_USER));
    };
  }

  private @Nullable PasswordCandidate getOldPasswordFromInput(Input input) {
    return input instanceof Input.Id inputId
        ? inputId.oldPassword()
        : ((Input.UserName) input).oldPassword();
  }

  private RawPassword getPasswordFromInput(Input input) {
    return input instanceof Input.Id inputId
        ? inputId.newPassword()
        : ((Input.UserName) input).newPassword();
  }

  private void updateCredentialOf(
      User user, @Nullable PasswordCandidate oldPassword, RawPassword newPassword) {
    credentialRepository
        .find(user.getCredential())
        .ifPresentOrElse(
            cred -> updateAndPersistCredential(cred, oldPassword, newPassword),
            IllegalDomainStateException.throwWith("A user is missing credential"));
  }

  private void updateAndPersistCredential(
      Credential credential, @Nullable PasswordCandidate oldPassword, RawPassword newPassword) {
    if (oldPassword == null
        || !passwordHasher.verify(oldPassword, credential.getHashedPassword())) {
      throw new ApplicationConstraintException(UpdateUserPasswordError.INVALID_OLD_PASSWORD);
    }

    final var hashedNewPassword = passwordHasher.hash(newPassword);

    credential.updatePassword(hashedNewPassword);
    credentialRepository.persist(credential);
  }
}

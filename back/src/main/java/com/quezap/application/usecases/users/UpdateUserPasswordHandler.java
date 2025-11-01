package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.users.UpdateUserPassword.Input;
import com.quezap.application.ports.users.UpdateUserPassword.Output;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.errors.users.UpdateUserPasswordError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

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
    final var newPassword = getPasswordFromInput(input);

    userRepository
        .find(userId)
        .ifPresentOrElse(
            user -> updateCredentialOf(user, newPassword),
            DomainConstraintException.throwWith(DeleteUserError.NO_SUCH_USER));

    return new Output.PasswordUpdated();
  }

  private UserId getUserIdFromInput(Input input) {
    return switch (input) {
      case Input.Id inputId -> inputId.id();
      case Input.UserName inputName ->
          userRepository
              .findByName(inputName.name())
              .<UserId>map(User::getId)
              .orElseThrow(DomainConstraintException.with(UpdateUserPasswordError.NO_SUCH_USER));
    };
  }

  private RawPassword getPasswordFromInput(Input input) {
    return input instanceof Input.Id inputId
        ? inputId.newPassword()
        : ((Input.UserName) input).newPassword();
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

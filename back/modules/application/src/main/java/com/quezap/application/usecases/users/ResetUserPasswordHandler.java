package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.ResetUserPasswordError;
import com.quezap.application.ports.users.ResetUserPassword.Input;
import com.quezap.application.ports.users.ResetUserPassword.Output;
import com.quezap.application.ports.users.ResetUserPassword.ResetUserPasswordUsecase;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class ResetUserPasswordHandler implements ResetUserPasswordUsecase {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final PasswordHasher passwordHasher;

  public ResetUserPasswordHandler(
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.credentialRepository = credentialRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final User user = findTargetUser(input);
    final RawPassword newPassword = extractNewPassword(input);

    final var credential =
        credentialRepository
            .find(user.getCredential())
            .orElseThrow(() -> new IllegalDomainStateException("A user is missing credential"));

    final var hashedNewPassword = passwordHasher.hash(newPassword);
    credential.updatePassword(hashedNewPassword);
    credentialRepository.persist(credential);

    return new Output.PasswordReset();
  }

  private User findTargetUser(Input input) {
    return switch (input) {
      case Input.Id(var id, var ignored) ->
          userRepository
              .find(id)
              .orElseThrow(
                  ApplicationConstraintException.with(ResetUserPasswordError.NO_SUCH_USER));
      case Input.UserName(var name, var ignored) ->
          userRepository
              .findByName(name)
              .orElseThrow(
                  ApplicationConstraintException.with(ResetUserPasswordError.NO_SUCH_USER));
    };
  }

  private RawPassword extractNewPassword(Input input) {
    return switch (input) {
      case Input.Id(var ignored, var newPassword) -> newPassword;
      case Input.UserName(var ignored, var newPassword) -> newPassword;
    };
  }
}

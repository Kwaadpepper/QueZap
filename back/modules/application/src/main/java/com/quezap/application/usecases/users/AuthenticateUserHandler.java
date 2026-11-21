package com.quezap.application.usecases.users;

import java.util.List;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AuthenticateUserError;
import com.quezap.application.ports.users.AuthenticateUser.AuthenticateUserUsecase;
import com.quezap.application.ports.users.AuthenticateUser.Input;
import com.quezap.application.ports.users.AuthenticateUser.Output;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.ports.directories.UserDirectory;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.pagination.Pagination;

@Usecase
final class AuthenticateUserHandler implements AuthenticateUserUsecase {
  private static final long PAGE_SIZE = 50L;

  private final CredentialRepository credentialRepository;
  private final UserRepository userRepository;
  private final UserDirectory userDirectory;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;

  public AuthenticateUserHandler(
      CredentialRepository credentialRepository,
      UserRepository userRepository,
      UserDirectory userDirectory,
      IdentifierHasher identifierHasher,
      PasswordHasher passwordHasher) {
    this.credentialRepository = credentialRepository;
    this.userRepository = userRepository;
    this.userDirectory = userDirectory;
    this.identifierHasher = identifierHasher;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var hashedIdentifier = identifierHasher.hash(input.identifier());
    final var credential =
        credentialRepository
            .findByIdentifier(hashedIdentifier)
            .orElseThrow(
                ApplicationConstraintException.with(AuthenticateUserError.INVALID_CREDENTIALS));

    final var passwordMatches =
        passwordHasher.verify(input.password(), credential.getHashedPassword());
    if (!passwordMatches) {
      throw new ApplicationConstraintException(AuthenticateUserError.INVALID_CREDENTIALS);
    }

    if (!credential.isActivated()) {
      throw new ApplicationConstraintException(AuthenticateUserError.ACCOUNT_NOT_ACTIVATED);
    }

    final var user =
        userRepository
            .findByCredential(credential.getId())
            .or(() -> findUserByCredentialId(credential.getId()))
            .orElseThrow(ApplicationConstraintException.with(AuthenticateUserError.USER_NOT_FOUND));

    return new Output(user.getId(), user.getName());
  }

  private java.util.Optional<User> findUserByCredentialId(CredentialId credentialId) {
    var pageNumber = 1L;
    while (true) {
      final var page = userDirectory.paginate(Pagination.ofPage(pageNumber, PAGE_SIZE));
      final List<UserView> items = page.items();
      if (items == null || items.isEmpty()) {
        return java.util.Optional.empty();
      }
      for (final var view : items) {
        final var userOpt = userRepository.find(view.id());
        if (userOpt.isPresent() && credentialId.equals(userOpt.get().getCredential())) {
          return userOpt;
        }
      }
      if (!page.hasNextPage()) {
        return java.util.Optional.empty();
      }
      pageNumber++;
    }
  }
}

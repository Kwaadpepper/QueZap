package com.quezap.application.usecases.users;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.AddUser.Input;
import com.quezap.application.ports.users.AddUser.Output;
import com.quezap.domain.errors.users.AddUserError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class AddUserHandler implements AddUserUsecase {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;

  public AddUserHandler(
      UserRepository userRepository,
      CredentialRepository credentialRepository,
      IdentifierHasher identifierHasher,
      PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.identifierHasher = identifierHasher;
    this.passwordHasher = passwordHasher;
    this.credentialRepository = credentialRepository;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var userName = input.name();
    final var identifier = input.identifier();
    final var rawPassword = input.password();
    final var hashedIdentifier = identifierHasher.hash(identifier);
    final var hashedPassword = passwordHasher.hash(rawPassword);

    if (credentialRepository.findByIdentifier(hashedIdentifier).isPresent()) {
      throw new DomainConstraintException(AddUserError.IDENTIFIER_ALREADY_TAKEN);
    }

    if (userRepository.findByName(userName).isPresent()) {
      throw new DomainConstraintException(AddUserError.USER_NAME_ALREADY_TAKEN);
    }

    final var credential = new Credential(hashedPassword, hashedIdentifier);
    final var user = new User(userName, credential.getId());

    credentialRepository.persist(credential);
    userRepository.persist(user);

    return new Output.UserAdded(user.getId());
  }
}

package com.quezap.domain.usecases.users;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.quezap.domain.errors.users.AddUserError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.IdentifierHasher;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

public sealed interface AddUser {
  record Input(String name, RawIdentifier identifier, RawPassword password)
      implements UseCaseInput {}

  sealed interface Output extends UseCaseOutput {
    record UserAdded() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, AddUser {
    private final UserRepository userRepository;
    private final IdentifierHasher identifierHasher;
    private final PasswordHasher passwordHasher;
    private final CredentialRepository credentialRepository;

    public Handler(
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
    public Output handle(Input usecaseInput) {
      final var userName = usecaseInput.name();
      final var identifier = usecaseInput.identifier();
      final var rawPassword = usecaseInput.password();
      final var hashedIdentifier = identifierHasher.hash(identifier);
      final var hashedPassword = passwordHasher.hash(rawPassword);

      if (credentialRepository.findByidentifier(hashedIdentifier) != null) {
        throw new DomainConstraintException(AddUserError.IDENTIFIER_ALREADY_TAKEN);
      }

      if (userRepository.findByName(userName) != null) {
        throw new DomainConstraintException(AddUserError.USER_NAME_ALREADY_TAKEN);
      }

      final var credential =
          new Credential(
              hashedPassword, hashedIdentifier, null, ZonedDateTime.now(ZoneId.of("UTC")));
      final var user =
          new User(userName, credential.getCredentialId(), ZonedDateTime.now(ZoneId.of("UTC")));

      credentialRepository.save(credential);
      userRepository.save(user);

      return new Output.UserAdded();
    }
  }
}

package com.quezap.domain.usecases.users;

import com.quezap.domain.errors.users.AddUserError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.port.repositories.CredentialRepository;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.domain.port.services.IdentifierHasher;
import com.quezap.domain.port.services.PasswordHasher;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddUserTest {
  private final AddUser.Handler handler;
  private final CredentialRepository credentialRepository;
  private final UserRepository userRepository;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;

  public AddUserTest() {
    credentialRepository = Mockito.mock(CredentialRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    identifierHasher = Mockito.mock(IdentifierHasher.class);
    passwordHasher = Mockito.mock(PasswordHasher.class);

    handler =
        new AddUser.Handler(userRepository, identifierHasher, passwordHasher, credentialRepository);
  }

  @Test
  void canCreateUser() {
    // GIVEN
    var userName = "some-username";
    var identifier = new RawIdentifier("some-id");
    var password = new RawPassword("Some-password1!");
    final var input = new AddUser.Input(userName, identifier, password);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier))
        .thenReturn(new HashedIdentifier("hashedIdentifier"));
    Mockito.when(passwordHasher.hash(password)).thenReturn(new HashedPassword("$2a$10$a"));
    Mockito.when(credentialRepository.findByidentifier(Mockito.any(HashedIdentifier.class)))
        .thenReturn(null);
    Mockito.when(userRepository.findByName(userName)).thenReturn(null);

    handler.handle(input);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void cannotCreateUserWhenIdentifierAlreadyExists() {
    // GIVEN
    var userName = "some-username";
    var identifier = new RawIdentifier("some-id");
    var password = new RawPassword("Some-password1!");
    var hashedIdentifier = new HashedIdentifier("some-hashed-id");
    final var input = new AddUser.Input(userName, identifier, password);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier)).thenReturn(hashedIdentifier);
    Mockito.when(credentialRepository.findByidentifier(hashedIdentifier))
        .thenReturn(Mockito.mock(Credential.class));

    // THEN
    Assertions.assertThatThrownBy(() -> handler.handle(input))
        .isInstanceOf(DomainConstraintException.class)
        .hasMessage(AddUserError.IDENTIFIER_ALREADY_TAKEN.getMessage());
  }

  @Test
  void cannotCreateUserWhenUsernameAlreadyExists() {
    // GIVEN
    var userName = "some-username";
    var identifier = new RawIdentifier("some-id");
    var password = new RawPassword("Some-password1!");
    var hashedIdentifier = new HashedIdentifier("some-hashed-id");
    final var input = new AddUser.Input(userName, identifier, password);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier)).thenReturn(hashedIdentifier);
    Mockito.when(credentialRepository.findByidentifier(hashedIdentifier)).thenReturn(null);
    Mockito.when(userRepository.findByName(userName)).thenReturn(Mockito.mock(User.class));

    // THEN
    Assertions.assertThatThrownBy(() -> handler.handle(input))
        .isInstanceOf(DomainConstraintException.class)
        .hasMessage(AddUserError.USER_NAME_ALREADY_TAKEN.getMessage());
  }
}

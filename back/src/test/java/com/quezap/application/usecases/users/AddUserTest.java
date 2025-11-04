package com.quezap.application.usecases.users;

import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AddUserError;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.AddUser.Input;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddUserTest {
  private final AddUserUsecase usecase;
  private final CredentialRepository credentialRepository;
  private final UserRepository userRepository;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;

  public AddUserTest() {
    credentialRepository = MockEntity.mock(CredentialRepository.class);
    userRepository = MockEntity.mock(UserRepository.class);
    identifierHasher = MockEntity.mock(IdentifierHasher.class);
    passwordHasher = MockEntity.mock(PasswordHasher.class);

    usecase =
        new AddUserHandler(userRepository, credentialRepository, identifierHasher, passwordHasher);
  }

  @Test
  void canCreateUser() {
    // GIVEN
    var userName = "some-username";
    var identifier = new RawIdentifier("some-id");
    var password = new RawPassword("Some-password1!");
    final var input = new Input(userName, identifier, password);
    final var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier))
        .thenReturn(new HashedIdentifier("hashedIdentifier"));
    Mockito.when(passwordHasher.hash(password)).thenReturn(new HashedPassword("$2a$10$a"));
    Mockito.when(credentialRepository.findByIdentifier(MockEntity.any(HashedIdentifier.class)))
        .thenReturn(MockEntity.optional());
    Mockito.when(userRepository.findByName(userName)).thenReturn(MockEntity.optional());

    usecase.handle(input, unitOfWork);

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
    final var input = new Input(userName, identifier, password);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier)).thenReturn(hashedIdentifier);
    Mockito.when(credentialRepository.findByIdentifier(hashedIdentifier))
        .thenReturn(MockEntity.optional(Credential.class));

    // THEN
    Assertions.assertThatThrownBy(() -> usecase.handle(input, unitOfWork))
        .isInstanceOf(ApplicationConstraintException.class)
        .hasMessage(AddUserError.IDENTIFIER_ALREADY_TAKEN.getMessage());
  }

  @Test
  void cannotCreateUserWhenUsernameAlreadyExists() {
    // GIVEN
    var userName = "some-username";
    var identifier = new RawIdentifier("some-id");
    var password = new RawPassword("Some-password1!");
    var hashedIdentifier = new HashedIdentifier("some-hashed-id");
    final var input = new Input(userName, identifier, password);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(identifierHasher.hash(identifier)).thenReturn(hashedIdentifier);
    Mockito.when(credentialRepository.findByIdentifier(hashedIdentifier))
        .thenReturn(MockEntity.optional());
    Mockito.when(userRepository.findByName(userName)).thenReturn(MockEntity.optional(User.class));

    // THEN
    Assertions.assertThatThrownBy(() -> usecase.handle(input, unitOfWork))
        .isInstanceOf(ApplicationConstraintException.class)
        .hasMessage(AddUserError.USER_NAME_ALREADY_TAKEN.getMessage());
  }
}

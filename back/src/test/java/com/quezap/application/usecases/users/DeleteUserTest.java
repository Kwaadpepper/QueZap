package com.quezap.application.usecases.users;

import java.util.UUID;

import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.DeleteUser.Input;
import com.quezap.application.ports.users.DeleteUser.Output;
import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteUserTest {
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final DeleteUserUsecase usecase;

  public DeleteUserTest() {
    userRepository = MockEntity.mock(UserRepository.class);
    credentialRepository = MockEntity.mock(CredentialRepository.class);
    usecase = new DeleteUserHandler(userRepository, credentialRepository);
  }

  @Test
  void canDeleteUserById() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var credentialId = new CredentialId(UUID.fromString("117f5a80-7e6d-7e6e-0000-000000000000"));
    var user = MockEntity.mock(User.class);
    var input = new Input.Id(userId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(user.getId()).thenReturn(userId);
    Mockito.when(user.getCredential()).thenReturn(credentialId);
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional(user));
    Mockito.when(credentialRepository.find(credentialId))
        .thenReturn(MockEntity.optional(Credential.class));

    var output = usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertInstanceOf(Output.UserDeleted.class, output);
  }

  @Test
  void canDeleteUserByName() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var credentialId = new CredentialId(UUID.fromString("117f5a80-7e6d-7e6e-0000-000000000000"));
    var userName = "test";
    var user = MockEntity.mock(User.class);
    var input = new Input.UserName(userName);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(userRepository.findByName(userName)).thenReturn(MockEntity.optional(user));
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional(user));
    Mockito.when(user.getId()).thenReturn(userId);
    Mockito.when(user.getCredential()).thenReturn(credentialId);

    Mockito.when(credentialRepository.find(credentialId))
        .thenReturn(MockEntity.optional(Credential.class));

    var output = usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertInstanceOf(Output.UserDeleted.class, output);
  }

  @Test
  void canDeleteUserThatIsMissingCredential() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var credentialId = new CredentialId(UUID.fromString("117f5a80-7e6d-7e6e-0000-000000000000"));
    var user = MockEntity.mock(User.class);
    var input = new Input.Id(userId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(user.getId()).thenReturn(userId);
    Mockito.when(user.getCredential()).thenReturn(credentialId);
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional(user));
    Mockito.when(credentialRepository.find(credentialId)).thenReturn(MockEntity.optional());

    var output = usecase.handle(input, unitOfWork);

    // THEN
    Assertions.assertInstanceOf(Output.UserDeleted.class, output);
  }

  @Test
  void cannotDeleteUnknownUserById() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new Input.Id(userId);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN / THEN
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional());

    var exception =
        Assertions.assertThrows(RuntimeException.class, () -> usecase.handle(input, unitOfWork));
    Assertions.assertEquals(DeleteUserError.NO_SUCH_USER.getMessage(), exception.getMessage());
  }
}

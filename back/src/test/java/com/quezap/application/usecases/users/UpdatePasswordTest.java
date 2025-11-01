package com.quezap.application.usecases.users;

import java.util.UUID;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.mocks.MockEntity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UpdatePasswordTest {
  private final UpdateUserPassword.Handler handler;
  private final UserRepository userRepository;
  private final CredentialRepository credentialRepository;
  private final PasswordHasher passwordHasher;

  public UpdatePasswordTest() {
    userRepository = MockEntity.mock(UserRepository.class);
    credentialRepository = MockEntity.mock(CredentialRepository.class);
    passwordHasher = MockEntity.mock(PasswordHasher.class);
    handler = new UpdateUserPassword.Handler(userRepository, credentialRepository, passwordHasher);
  }

  @Test
  void canUpdatePasswordUsingId() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6f-0000-000000000000"));
    var newPassword = new RawPassword("P4assw0rd.");
    final var input = new UpdateUserPassword.Input.Id(userId, newPassword);
    final var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    var user = MockEntity.mock(User.class);
    Mockito.when(user.getCredential()).thenReturn(credentialId);
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional(user));
    Mockito.when(credentialRepository.find(credentialId))
        .thenReturn(MockEntity.optional(Credential.class));
    Mockito.when(passwordHasher.hash(newPassword))
        .thenReturn(MockEntity.mock(HashedPassword.class));

    handler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void canUpdatePasswordUsingName() {
    // GIVEN
    var userName = "johndoe";
    var credentialId = new CredentialId(UUID.fromString("017f5a80-7e6d-7e6f-0000-000000000000"));
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var newPassword = new RawPassword("P4assw0rd.");
    final var input = new UpdateUserPassword.Input.UserName(userName, newPassword);
    final var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    var user = MockEntity.mock(User.class);
    Mockito.when(user.getId()).thenReturn(userId);
    Mockito.when(user.getCredential()).thenReturn(credentialId);
    Mockito.when(userRepository.findByName(userName)).thenReturn(MockEntity.optional(user));
    Mockito.when(userRepository.find(Mockito.any())).thenReturn(MockEntity.optional(user));
    Mockito.when(credentialRepository.find(credentialId))
        .thenReturn(MockEntity.optional(Credential.class));
    Mockito.when(passwordHasher.hash(newPassword))
        .thenReturn(MockEntity.mock(HashedPassword.class));

    handler.handle(input, unitOfWork);

    // THEN
    Assertions.assertThatCode(() -> {}).doesNotThrowAnyException();
  }

  @Test
  void cannotUpdatePasswordForUnknownUser() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var newPassword = new RawPassword("P4assw0rd.");
    final var input = new UpdateUserPassword.Input.Id(userId, newPassword);
    var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(userRepository.find(userId)).thenReturn(MockEntity.optional());
    var thrown = Assertions.catchThrowable(() -> handler.handle(input, unitOfWork));

    // THEN
    Assertions.assertThat(thrown).isInstanceOf(Exception.class);
  }

  @Test
  void cannotUpdatePasswordForUnknownUserName() {
    // GIVEN
    var userName = "johndoe";
    var newPassword = new RawPassword("P4assw0rd.");
    final var input = new UpdateUserPassword.Input.UserName(userName, newPassword);
    final var unitOfWork = MockEntity.mock(UnitOfWorkEvents.class);

    // WHEN
    Mockito.when(userRepository.findByName(userName)).thenReturn(MockEntity.optional());
    var thrown = Assertions.catchThrowable(() -> handler.handle(input, unitOfWork));

    // THEN
    Assertions.assertThat(thrown).isInstanceOf(Exception.class);
  }
}

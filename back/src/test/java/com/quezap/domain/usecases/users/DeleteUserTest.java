package com.quezap.domain.usecases.users;

import java.util.UUID;

import com.quezap.domain.errors.users.DeleteUserError;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.UserRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteUserTest {
  private final UserRepository userRepository;
  private final DeleteUser.Handler deleteUserHandler;

  public DeleteUserTest() {
    userRepository = Mockito.mock(UserRepository.class);
    deleteUserHandler = new DeleteUser.Handler(userRepository);
  }

  @Test
  void canDeleteUserById() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var user = Mockito.mock(User.class);
    var input = new DeleteUser.Input.Id(userId);

    // WHEN
    Mockito.when(userRepository.find(userId.value())).thenReturn(user);

    var output = deleteUserHandler.handle(input);

    // THEN
    Assertions.assertInstanceOf(DeleteUser.Output.UserDeleted.class, output);
  }

  @Test
  void canDeleteUserByName() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var userName = "test";
    var user = Mockito.mock(User.class);
    var input = new DeleteUser.Input.UserName(userName);

    // WHEN
    Mockito.when(userRepository.findByName(userName)).thenReturn(user);
    Mockito.when(userRepository.find(userId.value())).thenReturn(user);
    Mockito.when(user.getId()).thenReturn(userId.value());

    var output = deleteUserHandler.handle(input);

    // THEN
    Assertions.assertInstanceOf(DeleteUser.Output.UserDeleted.class, output);
  }

  @Test
  void cannotDeleteUnknownUserById() {
    // GIVEN
    var userId = new UserId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var input = new DeleteUser.Input.Id(userId);

    // WHEN / THEN
    Mockito.when(userRepository.find(userId.value())).thenReturn(null);

    var exception =
        Assertions.assertThrows(RuntimeException.class, () -> deleteUserHandler.handle(input));
    Assertions.assertEquals(DeleteUserError.NO_SUCH_USER.getMessage(), exception.getMessage());
  }
}

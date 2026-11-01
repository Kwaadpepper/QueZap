package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.quezap.api.v1.dto.request.users.CreateUserRequest;
import com.quezap.api.v1.dto.request.users.UpdatePasswordRequest;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.users.UserController;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserControllerTest {

  private UsecaseExecutor executor;
  private AddUserUsecase addUserUsecase;
  private ListUsersUsecase listUsersUsecase;
  private UpdateUserPasswordUsecase updatePasswordUsecase;
  private DeleteUserUsecase deleteUserUsecase;
  private UserRepository userRepository;
  private PaginationMapper paginationMapper;
  private UserController userController;

  @BeforeEach
  void setUp() {
    executor = Mockito.mock(UsecaseExecutor.class);
    addUserUsecase = Mockito.mock(AddUserUsecase.class);
    listUsersUsecase = Mockito.mock(ListUsersUsecase.class);
    updatePasswordUsecase = Mockito.mock(UpdateUserPasswordUsecase.class);
    deleteUserUsecase = Mockito.mock(DeleteUserUsecase.class);
    userRepository = Mockito.mock(UserRepository.class);
    paginationMapper = new PaginationMapper();
    userController =
        new UserController(
            executor,
            addUserUsecase,
            listUsersUsecase,
            updatePasswordUsecase,
            deleteUserUsecase,
            userRepository,
            paginationMapper);
  }

  @Test
  void shouldCreateUser() {
    final var userId = new UserId(UuidV7.randomUuid());
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenReturn(new AddUser.Output.UserAdded(userId));

    final var response =
        userController.create(new CreateUserRequest("Alice", "alice123", "Password123!"));

    assertThat(response.id()).isEqualTo(userId.value());
  }

  @Test
  void shouldGenerateUniqueSecurePasswordWhenPasswordNotProvided() {
    final var req1 = new CreateUserRequest("Alice", "alice@example.com", "alice@example.com", null);
    final var req2 = new CreateUserRequest("Bob", "bob@example.com", "bob@example.com", "");

    final var pass1 = req1.getEffectivePassword();
    final var pass2 = req2.getEffectivePassword();

    assertThat(pass1).isNotBlank().isNotEqualTo("Password123!").isNotEqualTo(pass2);
    assertThat(pass1).hasSizeGreaterThanOrEqualTo(16);

    final var userId = new UserId(UuidV7.randomUuid());
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenReturn(new AddUser.Output.UserAdded(userId));

    final var response = userController.create(req1);
    assertThat(response.id()).isEqualTo(userId.value());
  }

  @Test
  void shouldGetUserById() {
    final var userId = UuidV7.randomUuid();
    final var user = new User("Alice", new CredentialId(UuidV7.randomUuid()));
    when(userRepository.find(new UserId(userId))).thenReturn(Optional.of(user));

    final var response = userController.getById(userId);

    assertThat(response.name()).isEqualTo("Alice");
  }

  @Test
  void shouldUpdatePassword() {
    final var userId = UuidV7.randomUuid();

    userController.updatePassword(userId, new UpdatePasswordRequest("OldPass123!", "NewPass456!"));

    verify(executor).execute(eq(updatePasswordUsecase), any());
  }

  @Test
  void shouldDeleteUser() {
    final var userId = UuidV7.randomUuid();

    userController.delete(userId);

    verify(executor).execute(eq(deleteUserUsecase), any());
  }
}

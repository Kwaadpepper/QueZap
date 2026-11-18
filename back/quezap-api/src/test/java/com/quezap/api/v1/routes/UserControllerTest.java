package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.users.CreateUserRequest;
import com.quezap.api.v1.dto.request.users.UpdatePasswordRequest;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.api.v1.routes.users.UserController;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserControllerTest {

  private UsecaseExecutor executor;
  private AddUserUsecase addUserUsecase;
  private FindUserUsecase findUserUsecase;
  private ListUsersUsecase listUsersUsecase;
  private UpdateUserPasswordUsecase updatePasswordUsecase;
  private DeleteUserUsecase deleteUserUsecase;
  private PaginationMapper paginationMapper;
  private UserController userController;

  @BeforeEach
  void setUp() {
    executor = Mockito.mock(UsecaseExecutor.class);
    addUserUsecase = Mockito.mock(AddUserUsecase.class);
    findUserUsecase = Mockito.mock(FindUserUsecase.class);
    listUsersUsecase = Mockito.mock(ListUsersUsecase.class);
    updatePasswordUsecase = Mockito.mock(UpdateUserPasswordUsecase.class);
    deleteUserUsecase = Mockito.mock(DeleteUserUsecase.class);
    paginationMapper = new PaginationMapper();
    userController =
        new UserController(
            executor,
            addUserUsecase,
            findUserUsecase,
            listUsersUsecase,
            updatePasswordUsecase,
            deleteUserUsecase,
            paginationMapper);
  }

  private Authentication userAuth(UserId userId) {
    return new UsernamePasswordAuthenticationToken(
        userId.value().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private Authentication adminAuth(UserId userId) {
    return new UsernamePasswordAuthenticationToken(
        userId.value().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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
  void shouldRejectCreateUserWhenPasswordIsBlank() {
    final var req = new CreateUserRequest("Alice", "alice@example.com", "alice@example.com", "");

    assertThatThrownBy(() -> userController.create(req))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldGetOwnUserById() {
    final var userId = new UserId(UuidV7.randomUuid());
    final var now = TimelinePoint.now();
    when(executor.execute(eq(findUserUsecase), any(FindUser.Input.class)))
        .thenReturn(new FindUser.Output(userId, "Alice", now, now));

    final var response = userController.getById(userId.value(), userAuth(userId));

    assertThat(response.name()).isEqualTo("Alice");
  }

  @Test
  void shouldRejectGetOtherUserByIdWith403() {
    final var targetId = new UserId(UuidV7.randomUuid());
    final var callerId = new UserId(UuidV7.randomUuid());

    assertThatThrownBy(() -> userController.getById(targetId.value(), userAuth(callerId)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldListOnlyCurrentUserForNonAdmin() {
    final var callerId = new UserId(UuidV7.randomUuid());
    final var now = TimelinePoint.now();
    when(executor.execute(eq(findUserUsecase), any(FindUser.Input.class)))
        .thenReturn(new FindUser.Output(callerId, "Alice", now, now));

    final var page =
        userController.list(new PaginationDto(1L, 10L, null, null, null, null), userAuth(callerId));

    assertThat(page.data()).hasSize(1);
    assertThat(page.data().getFirst().id()).isEqualTo(callerId);
  }

  @Test
  void shouldAllowAdminToListAllUsers() {
    final var adminId = new UserId(UuidV7.randomUuid());
    final var now = TimelinePoint.now();
    final var dto = new ListUsers.Output.UserDto(adminId, "Admin", now, now);
    when(executor.execute(eq(listUsersUsecase), any(ListUsers.Input.class)))
        .thenReturn(new ListUsers.Output(PageOf.of(Pagination.firstPage(), List.of(dto), 1L)));

    final var page =
        userController.list(new PaginationDto(1L, 10L, null, null, null, null), adminAuth(adminId));

    assertThat(page.data()).hasSize(1);
    verify(executor).execute(eq(listUsersUsecase), any(ListUsers.Input.class));
  }

  @Test
  void shouldUpdateOwnPassword() {
    final var userId = new UserId(UuidV7.randomUuid());

    userController.updatePassword(
        userId.value(), new UpdatePasswordRequest("OldPass123!", "NewPass456!"), userAuth(userId));

    verify(executor).execute(eq(updatePasswordUsecase), any());
  }

  @Test
  void shouldRejectUpdateOtherUserPasswordWith403() {
    final var targetId = new UserId(UuidV7.randomUuid());
    final var callerId = new UserId(UuidV7.randomUuid());

    assertThatThrownBy(
            () ->
                userController.updatePassword(
                    targetId.value(),
                    new UpdatePasswordRequest("OldPass123!", "NewPass456!"),
                    userAuth(callerId)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldDeleteOwnUser() {
    final var userId = new UserId(UuidV7.randomUuid());

    userController.delete(userId.value(), userAuth(userId));

    verify(executor).execute(eq(deleteUserUsecase), any());
  }

  @Test
  void shouldRejectDeleteOtherUserWith403() {
    final var targetId = new UserId(UuidV7.randomUuid());
    final var callerId = new UserId(UuidV7.randomUuid());

    assertThatThrownBy(() -> userController.delete(targetId.value(), userAuth(callerId)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }
}

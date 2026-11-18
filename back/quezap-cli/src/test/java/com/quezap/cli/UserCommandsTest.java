package com.quezap.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AddUserError;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;
import com.quezap.lib.utils.UuidV7;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserCommandsTest {

  private UsecaseExecutor executor;
  private ListUsersUsecase listUsersUsecase;
  private AddUserUsecase addUserUsecase;
  private DeleteUserUsecase deleteUserUsecase;
  private LineReader lineReader;
  private UserCommands userCommands;

  @BeforeEach
  void setUp() {
    executor = Mockito.mock(UsecaseExecutor.class);
    listUsersUsecase = Mockito.mock(ListUsersUsecase.class);
    addUserUsecase = Mockito.mock(AddUserUsecase.class);
    deleteUserUsecase = Mockito.mock(DeleteUserUsecase.class);
    lineReader = Mockito.mock(LineReader.class);
    userCommands = new UserCommands(executor, listUsersUsecase, addUserUsecase, deleteUserUsecase);
    userCommands.setLineReader(lineReader);
  }

  @Test
  void listUsersStopsWhenHasNextPageIsFalseOnSinglePage() {
    final var now = new TimelinePoint(Instant.parse("2026-01-01T00:00:00Z"));
    final var userId = new UserId(UuidV7.randomUuid());
    final var user = new ListUsers.Output.UserDto(userId, "alice", now, now);
    final var page = PageOf.of(Pagination.ofPage(1L, 10L), List.of(user), 1L);

    when(executor.execute(eq(listUsersUsecase), any(ListUsers.Input.class)))
        .thenReturn(new ListUsers.Output(page));

    final var result = userCommands.listUsers();

    assertThat(result).contains("Users list :").contains("- alice (" + userId + ")");
    verify(executor, times(1)).execute(eq(listUsersUsecase), any(ListUsers.Input.class));
  }

  @Test
  void listUsersFetchesAllPagesWhenHasNextPageIsTrue() {
    final var now = new TimelinePoint(Instant.parse("2026-01-01T00:00:00Z"));
    final var id1 = new UserId(UuidV7.randomUuid());
    final var id2 = new UserId(UuidV7.randomUuid());
    final var page1 =
        PageOf.of(
            Pagination.ofPage(1L, 10L),
            List.of(new ListUsers.Output.UserDto(id1, "alice", now, now)),
            15L);
    final var page2 =
        PageOf.of(
            Pagination.ofPage(2L, 10L),
            List.of(new ListUsers.Output.UserDto(id2, "bob", now, now)),
            15L);

    when(executor.execute(eq(listUsersUsecase), any(ListUsers.Input.class)))
        .thenReturn(new ListUsers.Output(page1), new ListUsers.Output(page2));

    final var result = userCommands.listUsers();

    assertThat(result).contains("- alice (" + id1 + ")").contains("- bob (" + id2 + ")");
    verify(executor, times(2)).execute(eq(listUsersUsecase), any(ListUsers.Input.class));
  }

  @Test
  void addUserMasksPasswordAndExecutesUsecase() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("StrongPass1!");
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenReturn(new AddUser.Output.UserAdded(new UserId(UuidV7.randomUuid())));

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).isEqualTo("Done");
    verify(lineReader).readLine("Mot de passe : ", '*');
    verify(executor).execute(eq(addUserUsecase), any(AddUser.Input.class));
  }

  @Test
  void addUserHandlesNullEofCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn(null);

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesEndOfFileExceptionCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenThrow(new EndOfFileException());

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesUserInterruptExceptionCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenThrow(new UserInterruptException(""));

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesWeakPasswordDomainError() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("weak");

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).startsWith("Error :");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesApplicationConstraintException() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("StrongPass1!");
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenThrow(new ApplicationConstraintException(AddUserError.IDENTIFIER_ALREADY_TAKEN));

    final var result = userCommands.addUser("alice", "alice@example.com");

    assertThat(result).startsWith("Error");
  }

  @Test
  void deleteUserByUuidAndByUsername() {
    final var uuid = UuidV7.randomUuid();
    when(executor.execute(eq(deleteUserUsecase), any(DeleteUser.Input.class)))
        .thenReturn(new DeleteUser.Output.UserDeleted());

    final var byIdResult = userCommands.deleteUser(uuid.toString());
    final var byNameResult = userCommands.deleteUser("alice");

    assertThat(byIdResult).isEqualTo("Done");
    assertThat(byNameResult).isEqualTo("Done");
    verify(executor).execute(eq(deleteUserUsecase), eq(new DeleteUser.Input.Id(new UserId(uuid))));
    verify(executor).execute(eq(deleteUserUsecase), eq(new DeleteUser.Input.UserName("alice")));
  }
}

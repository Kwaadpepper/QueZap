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
import com.quezap.application.ports.users.ActivateUser;
import com.quezap.application.ports.users.ActivateUser.ActivateUserUsecase;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.FindUserCredential;
import com.quezap.application.ports.users.FindUserCredential.FindUserCredentialUsecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.RenameUser;
import com.quezap.application.ports.users.RenameUser.RenameUserUsecase;
import com.quezap.application.ports.users.ResetUserPassword;
import com.quezap.application.ports.users.ResetUserPassword.ResetUserPasswordUsecase;
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
  private FindUserCredentialUsecase findUserCredentialUsecase;
  private AddUserUsecase addUserUsecase;
  private RenameUserUsecase renameUserUsecase;
  private ResetUserPasswordUsecase resetUserPasswordUsecase;
  private ActivateUserUsecase activateUserUsecase;
  private DeleteUserUsecase deleteUserUsecase;
  private LineReader lineReader;
  private UserCommands userCommands;

  @BeforeEach
  void setUp() {
    executor = Mockito.mock(UsecaseExecutor.class);
    listUsersUsecase = Mockito.mock(ListUsersUsecase.class);
    findUserCredentialUsecase = Mockito.mock(FindUserCredentialUsecase.class);
    addUserUsecase = Mockito.mock(AddUserUsecase.class);
    renameUserUsecase = Mockito.mock(RenameUserUsecase.class);
    resetUserPasswordUsecase = Mockito.mock(ResetUserPasswordUsecase.class);
    activateUserUsecase = Mockito.mock(ActivateUserUsecase.class);
    deleteUserUsecase = Mockito.mock(DeleteUserUsecase.class);
    lineReader = Mockito.mock(LineReader.class);
    userCommands =
        new UserCommands(
            executor,
            listUsersUsecase,
            findUserCredentialUsecase,
            addUserUsecase,
            renameUserUsecase,
            resetUserPasswordUsecase,
            activateUserUsecase,
            deleteUserUsecase);
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

    assertThat(result).contains("Users list :").contains("- alice (" + userId.value() + ")");
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

    assertThat(result)
        .contains("- alice (" + id1.value() + ")")
        .contains("- bob (" + id2.value() + ")");
    verify(executor, times(2)).execute(eq(listUsersUsecase), any(ListUsers.Input.class));
  }

  @Test
  void showUserByNameDisplaysDetailsAndActivationStatus() {
    final var now = new TimelinePoint(Instant.parse("2026-01-01T00:00:00Z"));
    final var userId = new UserId(UuidV7.randomUuid());
    when(executor.execute(eq(findUserCredentialUsecase), any(FindUserCredential.Input.class)))
        .thenReturn(
            new FindUserCredential.Output(userId, "alice", true, "0123456789abcdef", now, now));

    final var result = userCommands.showUser("alice");

    assertThat(result)
        .contains("ID: " + userId.value())
        .contains("Name: alice")
        .contains("Activated: true");
    verify(executor)
        .execute(eq(findUserCredentialUsecase), eq(new FindUserCredential.Input.ByName("alice")));
  }

  @Test
  void addUserMasksPasswordAndExecutesUsecase() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("StrongPass1!");
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenReturn(new AddUser.Output.UserAdded(new UserId(UuidV7.randomUuid())));

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).isEqualTo("Done");
    verify(lineReader).readLine("Mot de passe : ", '*');
    verify(executor).execute(eq(addUserUsecase), any(AddUser.Input.class));
  }

  @Test
  void addUserWithPasswordOptionSkipsPrompt() {
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenReturn(new AddUser.Output.UserAdded(new UserId(UuidV7.randomUuid())));

    final var result = userCommands.addUser("alice", "alice@example.com", "StrongPass1!");

    assertThat(result).isEqualTo("Done");
    verifyNoInteractions(lineReader);
    verify(executor).execute(eq(addUserUsecase), any(AddUser.Input.class));
  }

  @Test
  void addUserHandlesNullEofCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn(null);

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesEndOfFileExceptionCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenThrow(new EndOfFileException());

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesUserInterruptExceptionCleanly() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenThrow(new UserInterruptException(""));

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).startsWith("Error");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesWeakPasswordDomainError() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("weak");

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).startsWith("Error :");
    verifyNoInteractions(executor);
  }

  @Test
  void addUserHandlesApplicationConstraintException() {
    when(lineReader.readLine("Mot de passe : ", '*')).thenReturn("StrongPass1!");
    when(executor.execute(eq(addUserUsecase), any(AddUser.Input.class)))
        .thenThrow(new ApplicationConstraintException(AddUserError.IDENTIFIER_ALREADY_TAKEN));

    final var result = userCommands.addUser("alice", "alice@example.com", null);

    assertThat(result).startsWith("Error");
  }

  @Test
  void renameUserByIdAndByName() {
    final var uuid = UuidV7.randomUuid();
    when(executor.execute(eq(renameUserUsecase), any(RenameUser.Input.class)))
        .thenReturn(new RenameUser.Output.UserRenamed(new UserId(uuid), "alice-new"));

    assertThat(userCommands.renameUser(uuid.toString(), "alice-new")).isEqualTo("Done");
    assertThat(userCommands.renameUser("alice", "alice-new")).isEqualTo("Done");

    verify(executor)
        .execute(eq(renameUserUsecase), eq(new RenameUser.Input.Id(new UserId(uuid), "alice-new")));
    verify(executor)
        .execute(eq(renameUserUsecase), eq(new RenameUser.Input.UserName("alice", "alice-new")));
  }

  @Test
  void resetPasswordPromptsAndExecutesUsecase() {
    when(lineReader.readLine("Nouveau mot de passe : ", '*')).thenReturn("NewStrong1!");
    when(executor.execute(eq(resetUserPasswordUsecase), any(ResetUserPassword.Input.class)))
        .thenReturn(new ResetUserPassword.Output.PasswordReset());

    final var result = userCommands.resetPassword("alice", null);

    assertThat(result).isEqualTo("Done");
    verify(lineReader).readLine("Nouveau mot de passe : ", '*');
    verify(executor).execute(eq(resetUserPasswordUsecase), any(ResetUserPassword.Input.class));
  }

  @Test
  void activateUserByIdAndByName() {
    final var uuid = UuidV7.randomUuid();
    when(executor.execute(eq(activateUserUsecase), any(ActivateUser.Input.class)))
        .thenReturn(new ActivateUser.Output.UserActivated(new UserId(uuid)));

    assertThat(userCommands.activateUser(uuid.toString())).isEqualTo("Done");
    assertThat(userCommands.activateUser("alice")).isEqualTo("Done");

    verify(executor)
        .execute(eq(activateUserUsecase), eq(new ActivateUser.Input.Id(new UserId(uuid))));
    verify(executor).execute(eq(activateUserUsecase), eq(new ActivateUser.Input.UserName("alice")));
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

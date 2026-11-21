package com.quezap.cli;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.quezap.application.exceptions.ApplicationConstraintException;
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
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.pagination.Pagination;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jspecify.annotations.Nullable;

@Command(command = "users", description = "User management commands")
public class UserCommands {
  private final UsecaseExecutor executor;
  private final ListUsersUsecase listUsersUsecase;
  private final FindUserCredentialUsecase findUserCredentialUsecase;
  private final AddUserUsecase addUserUsecase;
  private final RenameUserUsecase renameUserUsecase;
  private final ResetUserPasswordUsecase resetUserPasswordUsecase;
  private final ActivateUserUsecase activateUserUsecase;
  private final DeleteUserUsecase deleteUserUsecase;

  @Autowired @Lazy private LineReader lineReader;

  public UserCommands(
      UsecaseExecutor executor,
      ListUsersUsecase listUsersUsecase,
      FindUserCredentialUsecase findUserCredentialUsecase,
      AddUserUsecase addUserUsecase,
      RenameUserUsecase renameUserUsecase,
      ResetUserPasswordUsecase resetUserPasswordUsecase,
      ActivateUserUsecase activateUserUsecase,
      DeleteUserUsecase deleteUserUsecase) {
    this.executor = executor;
    this.listUsersUsecase = listUsersUsecase;
    this.findUserCredentialUsecase = findUserCredentialUsecase;
    this.addUserUsecase = addUserUsecase;
    this.renameUserUsecase = renameUserUsecase;
    this.resetUserPasswordUsecase = resetUserPasswordUsecase;
    this.activateUserUsecase = activateUserUsecase;
    this.deleteUserUsecase = deleteUserUsecase;
  }

  void setLineReader(LineReader lineReader) {
    this.lineReader = lineReader;
  }

  @Command(command = "list", description = "List all users")
  public String listUsers() {
    final var output = new StringBuilder();
    output.append("Users list :\n");

    final var perPage = 10L;
    var pageNumber = 1L;
    boolean hasNextPage;
    do {
      final var pageRequest = Pagination.ofPage(pageNumber++, perPage);
      final var input = new ListUsers.Input(pageRequest);
      final var result = executor.execute(listUsersUsecase, input);
      final var paginatedUsers = result.page();

      final var items = paginatedUsers.items();
      for (ListUsers.Output.UserDto user : items) {
        output.append(formatUser(user)).append("\n");
      }
      hasNextPage = paginatedUsers.hasNextPage() && !items.isEmpty();
    } while (hasNextPage);

    return output.toString();
  }

  private String formatUser(ListUsers.Output.UserDto user) {
    return String.format(
        "- %s (%s) [created: %s]", user.name(), user.id().value(), user.createdAt().position());
  }

  @Command(command = "show", description = "Show details of a user")
  public String showUser(
      @Option(
              longNames = {"user", "login-or-id", "loginOrId"},
              shortNames = 'u',
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId) {
    try {
      final FindUserCredential.Input input =
          isUserId(loginOrId)
              ? new FindUserCredential.Input.ById(new UserId(UUID.fromString(loginOrId)))
              : new FindUserCredential.Input.ByName(loginOrId);

      final var user = executor.execute(findUserCredentialUsecase, input);
      return String.format(
          "ID: %s%nName: %s%nActivated: %s%nCreated at: %s%nUpdated at: %s",
          user.id().value(),
          user.name(),
          user.activated(),
          user.createdAt().position(),
          user.updatedAt().position());
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"add"},
      description = "Add a user (immediately activated)")
  String addUser(
      @Option(required = true, arity = CommandRegistration.OptionArity.EXACTLY_ONE) String name,
      @Option(required = true, arity = CommandRegistration.OptionArity.EXACTLY_ONE) String email,
      @Option(
              longNames = "password",
              required = false,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          @Nullable String passwordOption) {
    try {
      final var identifier = new RawIdentifier(email);
      final var rawPassword =
          passwordOption != null ? passwordOption : readPassword("Mot de passe : ");
      if (rawPassword == null) {
        return "Error : Password input cancelled";
      }
      final var password = new RawPassword(rawPassword);
      final var input = new AddUser.Input(name, identifier, password, true);

      executor.execute(addUserUsecase, input);

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"rename"},
      description = "Rename an existing user")
  String renameUser(
      @Option(
              longNames = {"user", "login-or-id", "loginOrId"},
              shortNames = 'u',
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId,
      @Option(
              longNames = {"new-name", "newName"},
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String newName) {
    try {
      final RenameUser.Input input =
          isUserId(loginOrId)
              ? new RenameUser.Input.Id(new UserId(UUID.fromString(loginOrId)), newName)
              : new RenameUser.Input.UserName(loginOrId, newName);

      executor.execute(renameUserUsecase, input);

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"reset-password"},
      description = "Reset a user's password")
  String resetPassword(
      @Option(
              longNames = {"user", "login-or-id", "loginOrId"},
              shortNames = 'u',
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId,
      @Option(
              longNames = "password",
              required = false,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          @Nullable String passwordOption) {
    try {
      final var rawPassword =
          passwordOption != null ? passwordOption : readPassword("Nouveau mot de passe : ");
      if (rawPassword == null) {
        return "Error : Password input cancelled";
      }
      final var password = new RawPassword(rawPassword);
      final ResetUserPassword.Input input =
          isUserId(loginOrId)
              ? new ResetUserPassword.Input.Id(new UserId(UUID.fromString(loginOrId)), password)
              : new ResetUserPassword.Input.UserName(loginOrId, password);

      executor.execute(resetUserPasswordUsecase, input);

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"activate"},
      description = "Activate a pending user account")
  String activateUser(
      @Option(
              longNames = {"user", "login-or-id", "loginOrId"},
              shortNames = 'u',
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId) {
    try {
      final ActivateUser.Input input =
          isUserId(loginOrId)
              ? new ActivateUser.Input.Id(new UserId(UUID.fromString(loginOrId)))
              : new ActivateUser.Input.UserName(loginOrId);

      executor.execute(activateUserUsecase, input);

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"delete"},
      description = "Delete a user")
  String deleteUser(
      @Option(
              longNames = {"user", "login-or-id", "loginOrId"},
              shortNames = 'u',
              required = true,
              arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId) {
    try {
      if (isUserId(loginOrId)) {
        final var newUserId = new UserId(UUID.fromString(loginOrId));
        final var input = new DeleteUser.Input.Id(newUserId);

        executor.execute(deleteUserUsecase, input);
      } else {
        final var input = new DeleteUser.Input.UserName(loginOrId);

        executor.execute(deleteUserUsecase, input);
      }

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (ApplicationConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  private @Nullable String readPassword(String prompt) {
    try {
      return lineReader.readLine(prompt, '*');
    } catch (EndOfFileException | UserInterruptException _) {
      return null;
    }
  }

  private boolean isUserId(String str) {
    try {
      new UserId(UUID.fromString(str));
      return true;
    } catch (IllegalDomainStateException | IllegalArgumentException _) {
      return false;
    }
  }
}

package com.quezap.application.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.usecases.users.AddUser;
import com.quezap.domain.usecases.users.DeleteUser;
import com.quezap.domain.usecases.users.ListUsers;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.pagination.Pagination;

import org.jline.reader.LineReader;

@Command(command = "users", description = "User management commands")
public class UserCommands {
  private final ListUsers.Handler listUsersHandler;
  private final AddUser.Handler addUserHandler;
  private final DeleteUser.Handler deleteUserHandler;

  @Autowired @Lazy private LineReader lineReader;

  public UserCommands(
      ListUsers.Handler listUsersHandler,
      AddUser.Handler addUserHandler,
      DeleteUser.Handler deleteUserHandler) {
    this.listUsersHandler = listUsersHandler;
    this.addUserHandler = addUserHandler;
    this.deleteUserHandler = deleteUserHandler;
  }

  @Command(command = "list", description = "list users")
  public String listUsers() {
    final var output = new StringBuilder();
    final var users = new ArrayList<ListUsers.Output.UserDto>();

    final var perPage = 10L;
    var pageNumber = 1L;
    List<ListUsers.Output.UserDto> pageUsers;
    do {
      final var pageRequest = Pagination.ofPage(pageNumber++, perPage);
      final var input = new ListUsers.Input(pageRequest);

      pageUsers = listUsersHandler.handle(input).items().items();
      users.addAll(pageUsers);
    } while (!pageUsers.isEmpty());

    output.append("Users list :\n");
    for (ListUsers.Output.UserDto user : users) {
      output.append(formatUser(user)).append("\n");
    }

    return output.toString();
  }

  private String formatUser(ListUsers.Output.UserDto user) {
    return String.format("- %s (%s)", user.name(), user.id());
  }

  @Command(
      command = {"add"},
      description = "Add a user")
  String addUser(
      @Option(required = true, arity = CommandRegistration.OptionArity.EXACTLY_ONE) String name,
      @Option(required = true, arity = CommandRegistration.OptionArity.EXACTLY_ONE) String login) {
    try {
      final var identifier = new RawIdentifier(login);
      final var password = new RawPassword(readPassword("Mot de passe : "));
      final var input = new AddUser.Input(name, identifier, password);

      addUserHandler.handle(input);

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (DomainConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  @Command(
      command = {"delete"},
      description = "Delete a user")
  String deleteUser(
      @Option(required = true, arity = CommandRegistration.OptionArity.EXACTLY_ONE)
          String loginOrId) {
    try {
      if (isUserId(loginOrId)) {
        final var input = new DeleteUser.Input.Id(new UserId(UUID.fromString(loginOrId)));
        deleteUserHandler.handle(input);
      } else {
        final var input = new DeleteUser.Input.UserName(loginOrId);
        deleteUserHandler.handle(input);
      }

      return "Done";
    } catch (IllegalDomainStateException e) {
      return "Error : " + e.getMessage();
    } catch (DomainConstraintException e) {
      return "Error " + e.getMessage();
    }
  }

  private String readPassword(String prompt) {
    return lineReader.readLine(prompt, null);
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

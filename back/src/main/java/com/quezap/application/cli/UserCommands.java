package com.quezap.application.cli;

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.quezap.domain.usecases.users.ListUsers;
import com.quezap.lib.pagination.PageRequest;

@ShellComponent
public class UserCommands {
  private final ListUsers.Handler listUsersHandler;

  public UserCommands(ListUsers.Handler listUsersHandler) {
    this.listUsersHandler = listUsersHandler;
  }

  @ShellMethod(key = "users:list", value = "liste les utilisateurs")
  public String listUsers() {
    final StringBuilder output = new StringBuilder();
    final var users = new ArrayList<ListUsers.Output.UserDto>();

    List<ListUsers.Output.UserDto> pageUsers;
    do {
      final var pageRequest = new PageRequest(1L, 10L);
      final var input = new ListUsers.Input(pageRequest);

      pageUsers = listUsersHandler.handle(input).items().items();
      users.addAll(pageUsers);
    } while (pageUsers.size() == 10);

    output.append("Liste des utilisateurs :\n");
    for (ListUsers.Output.UserDto user : users) {
      output.append("- ").append(user.name()).append(" (").append(user.id()).append(")\n");
    }

    return output.toString();
  }
}

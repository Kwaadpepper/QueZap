package com.quezap.application.usecases.users;

import com.quezap.application.anotations.Usecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.Input;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.ListUsers.Output;
import com.quezap.domain.ports.directories.UserDirectory;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;

@Usecase
final class ListUsersHandler implements ListUsersUsecase {
  private final UserDirectory userDirectory;

  public ListUsersHandler(UserDirectory userDirectory) {
    this.userDirectory = userDirectory;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var page = userDirectory.paginate(input.pagination());

    return new Output(page.map(this::toDto));
  }

  private ListUsers.Output.UserDto toDto(UserView user) {
    return new ListUsers.Output.UserDto(user.id(), user.name(), user.createdAt(), user.updatedAt());
  }
}

package com.quezap.application.usecases.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.directories.UserDirectory;
import com.quezap.domain.port.directories.views.UserView;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.NonNull;

public sealed interface ListUsers {
  record Input(Pagination pagination) implements UseCaseInput {}

  public record Output(PageOf<@NonNull UserDto> page) implements UseCaseOutput {
    public record UserDto(
        UserId id, String name, TimelinePoint createdAt, TimelinePoint updatedAt) {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, ListUsers {
    private final UserDirectory userDirectory;

    public Handler(UserDirectory userDirectory) {
      this.userDirectory = userDirectory;
    }

    @Override
    public Output handle(Input usecaseInput, UnitOfWorkEvents unitOfWork) {
      final var page = userDirectory.paginate(usecaseInput.pagination());

      return new Output(page.map(this::toDto));
    }

    private Output.UserDto toDto(UserView user) {
      return new Output.UserDto(user.id(), user.name(), user.createdAt(), user.updatedAt());
    }
  }
}

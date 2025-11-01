package com.quezap.application.ports.users;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.NonNull;

public interface ListUsers {
  record Input(Pagination pagination) implements UseCaseInput {}

  record Output(PageOf<@NonNull UserDto> page) implements UseCaseOutput {
    public record UserDto(
        UserId id, String name, TimelinePoint createdAt, TimelinePoint updatedAt) {}
  }

  public interface ListUsersUseCase extends UseCaseHandler<ListUsers.Input, ListUsers.Output> {}
}

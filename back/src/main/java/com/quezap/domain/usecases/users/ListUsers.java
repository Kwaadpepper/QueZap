package com.quezap.domain.usecases.users;

import java.time.ZonedDateTime;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public sealed interface ListUsers {
  record Input(Pagination pagination) implements UseCaseInput {}

  public record Output(PageOf<UserDto> items) implements UseCaseOutput {
    public record UserDto(
        UserId id, String name, ZonedDateTime createdAt, ZonedDateTime updatedAt) {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, ListUsers {
    private final UserRepository userRepository;

    public Handler(UserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var page = userRepository.findAll(usecaseInput.pagination());

      return new Output(page.map(this::toDto));
    }

    private Output.UserDto toDto(User user) {
      return new Output.UserDto(
          new UserId(user.getId()), user.getName(), user.getCreatedAt(), user.getUpdatedAt());
    }
  }
}

package com.quezap.domain.usecases.users;

import java.util.Optional;

import com.quezap.domain.models.valueobjects.UserId;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.UseCaseHandler;
import com.quezap.lib.ddd.UseCaseInput;
import com.quezap.lib.ddd.UseCaseOutput;
import com.quezap.lib.ddd.exceptions.DomainConstraintException;

public sealed interface DeleteUser {

  sealed interface Input extends UseCaseInput {
    record UserName(String name) implements Input {}

    record Id(UserId id) implements Input {}
  }

  sealed interface Output extends UseCaseOutput {
    record UserDeleted() implements Output {}
  }

  final class Handler implements UseCaseHandler<Input, Output>, DeleteUser {
    private final UserRepository userRepository;

    public Handler(UserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @Override
    public Output handle(Input usecaseInput) {
      final var userNotFoundException = new DomainConstraintException("User not found");
      final UserId userId =
          switch (usecaseInput) {
            case Input.Id(UserId id) -> id;
            case Input.UserName(String name) ->
                Optional.ofNullable(userRepository.findByName(name))
                    .map(AggregateRoot::getId)
                    .map(UserId::new)
                    .orElseThrow(() -> userNotFoundException);
          };
      final var user = userRepository.find(userId.value());

      if (user == null) {
        throw userNotFoundException;
      }

      userRepository.delete(user);

      return new Output.UserDeleted();
    }
  }
}

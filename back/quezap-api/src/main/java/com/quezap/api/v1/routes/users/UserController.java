package com.quezap.api.v1.routes.users;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.PaginationDto;
import com.quezap.api.v1.dto.request.users.CreateUserRequest;
import com.quezap.api.v1.dto.request.users.UpdatePasswordRequest;
import com.quezap.api.v1.dto.response.PageOfDto;
import com.quezap.api.v1.dto.response.users.UserDetailDto;
import com.quezap.api.v1.dto.response.users.UserIdDto;
import com.quezap.api.v1.mappers.PaginationMapper;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.UpdateUserPassword;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;

@RestController
@RequestMapping("apiv1/users")
public class UserController {

  private final UsecaseExecutor executor;
  private final AddUserUsecase addUserUsecase;
  private final ListUsersUsecase listUsersUsecase;
  private final UpdateUserPasswordUsecase updatePasswordUsecase;
  private final DeleteUserUsecase deleteUserUsecase;
  private final UserRepository userRepository;
  private final PaginationMapper paginationMapper;

  public UserController(
      UsecaseExecutor executor,
      AddUserUsecase addUserUsecase,
      ListUsersUsecase listUsersUsecase,
      UpdateUserPasswordUsecase updatePasswordUsecase,
      DeleteUserUsecase deleteUserUsecase,
      UserRepository userRepository,
      PaginationMapper paginationMapper) {
    this.executor = executor;
    this.addUserUsecase = addUserUsecase;
    this.listUsersUsecase = listUsersUsecase;
    this.updatePasswordUsecase = updatePasswordUsecase;
    this.deleteUserUsecase = deleteUserUsecase;
    this.userRepository = userRepository;
    this.paginationMapper = paginationMapper;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserIdDto create(@Valid @RequestBody CreateUserRequest request) {
    final var input =
        new AddUser.Input(
            request.name(),
            new RawIdentifier(request.identifier()),
            new RawPassword(request.password()));

    final var output = executor.execute(addUserUsecase, input);

    if (output instanceof AddUser.Output.UserAdded userAdded) {
      return new UserIdDto(userAdded.id().value());
    }

    throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected output from AddUser usecase");
  }

  @GetMapping("{id}")
  public UserDetailDto getById(@PathVariable UUID id) {
    final var user =
        userRepository
            .find(new UserId(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    return new UserDetailDto(
        user.getId().value(),
        user.getName(),
        user.getCreatedAt().position(),
        user.getUpdatedAt().position());
  }

  @GetMapping
  public PageOfDto<ListUsers.Output.UserDto> list(PaginationDto paginationDto) {
    final var pagination = paginationMapper.toDomain(paginationDto);
    final var input = new ListUsers.Input(pagination);
    final var output = executor.execute(listUsersUsecase, input);

    return paginationMapper.fromDomain(output.page(), dto -> dto);
  }

  @PutMapping("{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updatePassword(
      @PathVariable UUID id, @Valid @RequestBody UpdatePasswordRequest request) {
    final var input =
        new UpdateUserPassword.Input.Id(new UserId(id), new RawPassword(request.newPassword()));

    executor.execute(updatePasswordUsecase, input);
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    final var input = new DeleteUser.Input.Id(new UserId(id));

    executor.execute(deleteUserUsecase, input);
  }
}

package com.quezap.api.v1.routes.users;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.quezap.api.v1.services.AccountEmailService;
import com.quezap.api.v1.services.TurnstileService;
import com.quezap.application.ports.users.AddUser;
import com.quezap.application.ports.users.AddUser.AddUserUsecase;
import com.quezap.application.ports.users.DeleteUser;
import com.quezap.application.ports.users.DeleteUser.DeleteUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.application.ports.users.ListUsers;
import com.quezap.application.ports.users.ListUsers.ListUsersUsecase;
import com.quezap.application.ports.users.UpdateUserPassword;
import com.quezap.application.ports.users.UpdateUserPassword.UpdateUserPasswordUsecase;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("apiv1/users")
public class UserController {

  private static final long ACTIVATION_TOKEN_EXPIRATION_SECONDS = 60L * 60L * 24L; // 24 hours

  private final UsecaseExecutor executor;
  private final AddUserUsecase addUserUsecase;
  private final FindUserUsecase findUserUsecase;
  private final ListUsersUsecase listUsersUsecase;
  private final UpdateUserPasswordUsecase updatePasswordUsecase;
  private final DeleteUserUsecase deleteUserUsecase;
  private final PaginationMapper paginationMapper;
  private final @Nullable TurnstileService turnstileService;
  private final @Nullable AccountEmailService accountEmailService;
  private final @Nullable JwtService jwtService;

  @Autowired
  public UserController(
      UsecaseExecutor executor,
      AddUserUsecase addUserUsecase,
      FindUserUsecase findUserUsecase,
      ListUsersUsecase listUsersUsecase,
      UpdateUserPasswordUsecase updatePasswordUsecase,
      DeleteUserUsecase deleteUserUsecase,
      PaginationMapper paginationMapper,
      TurnstileService turnstileService,
      AccountEmailService accountEmailService,
      JwtService jwtService) {
    this.executor = executor;
    this.addUserUsecase = addUserUsecase;
    this.findUserUsecase = findUserUsecase;
    this.listUsersUsecase = listUsersUsecase;
    this.updatePasswordUsecase = updatePasswordUsecase;
    this.deleteUserUsecase = deleteUserUsecase;
    this.paginationMapper = paginationMapper;
    this.turnstileService = turnstileService;
    this.accountEmailService = accountEmailService;
    this.jwtService = jwtService;
  }

  public UserController(
      UsecaseExecutor executor,
      AddUserUsecase addUserUsecase,
      FindUserUsecase findUserUsecase,
      ListUsersUsecase listUsersUsecase,
      UpdateUserPasswordUsecase updatePasswordUsecase,
      DeleteUserUsecase deleteUserUsecase,
      PaginationMapper paginationMapper) {
    this.executor = executor;
    this.addUserUsecase = addUserUsecase;
    this.findUserUsecase = findUserUsecase;
    this.listUsersUsecase = listUsersUsecase;
    this.updatePasswordUsecase = updatePasswordUsecase;
    this.deleteUserUsecase = deleteUserUsecase;
    this.paginationMapper = paginationMapper;
    this.turnstileService = null;
    this.accountEmailService = null;
    this.jwtService = null;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserIdDto create(
      @Valid @RequestBody CreateUserRequest request, @Nullable HttpServletRequest httpRequest) {
    final var effName = request.getEffectiveName();
    final var effIdentifier = request.getEffectiveIdentifier();
    final var effPassword = request.getEffectivePassword();

    if (effIdentifier.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email/Identifier is required");
    }
    if (effPassword.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
    }

    if (turnstileService != null) {
      turnstileService.verifyOrThrow(request.turnstileToken(), extractClientIp(httpRequest));
    }

    final boolean requiresEmailActivation =
        accountEmailService != null && accountEmailService.isEnabled();
    final var input =
        new AddUser.Input(
            effName,
            new RawIdentifier(effIdentifier),
            new RawPassword(effPassword),
            !requiresEmailActivation);

    final var output = executor.execute(addUserUsecase, input);

    if (output instanceof AddUser.Output.UserAdded userAdded) {
      if (requiresEmailActivation && accountEmailService != null && jwtService != null) {
        sendActivationEmail(userAdded.id(), effIdentifier, effName);
      }
      return new UserIdDto(userAdded.id().value());
    }

    throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected output from AddUser usecase");
  }

  private void sendActivationEmail(UserId userId, String email, String userName) {
    if (accountEmailService == null || jwtService == null) {
      return;
    }
    try {
      final var exp = Instant.now().getEpochSecond() + ACTIVATION_TOKEN_EXPIRATION_SECONDS;
      final var payload =
          JwtToken.JwtPayload.builder(userId.value().toString(), exp)
              .jwtId(UUID.randomUUID().toString())
              .claim("type", "activation")
              .build();
      final var token = jwtService.generateToken(payload);
      accountEmailService.sendActivationEmail(email, userName, token);
    } catch (JwtService.JwtException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate activation token", e);
    }
  }

  @GetMapping("{id}")
  public UserDetailDto getById(@PathVariable UUID id, @Nullable Authentication authentication) {
    requireSelfOrAdmin(id, authentication);

    final var user = executor.execute(findUserUsecase, new FindUser.Input(new UserId(id)));

    return new UserDetailDto(
        user.id().value(), user.name(), user.createdAt().position(), user.updatedAt().position());
  }

  @GetMapping
  public PageOfDto<ListUsers.Output.UserDto> list(
      PaginationDto paginationDto, @Nullable Authentication authentication) {
    final var effectiveAuth = resolveAuthentication(authentication);
    if (isAdmin(effectiveAuth)) {
      final var pagination = paginationMapper.toDomain(paginationDto);
      final var input = new ListUsers.Input(pagination);
      final var output = executor.execute(listUsersUsecase, input);

      return paginationMapper.fromDomain(output.page(), dto -> dto);
    }

    final var currentUserId = extractCurrentUserId(effectiveAuth);
    final var user =
        executor.execute(findUserUsecase, new FindUser.Input(new UserId(currentUserId)));
    final var selfDto =
        new ListUsers.Output.UserDto(user.id(), user.name(), user.createdAt(), user.updatedAt());

    return new PageOfDto<>(List.of(selfDto), 1L, 1L, 1L, 1L, false, false);
  }

  @PutMapping("{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updatePassword(
      @PathVariable UUID id,
      @Valid @RequestBody UpdatePasswordRequest request,
      @Nullable Authentication authentication) {
    requireSelfOrAdmin(id, authentication);

    final var input =
        new UpdateUserPassword.Input.Id(
            new UserId(id),
            new PasswordCandidate(request.oldPassword()),
            new RawPassword(request.newPassword()));

    executor.execute(updatePasswordUsecase, input);
  }

  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, @Nullable Authentication authentication) {
    requireSelfOrAdmin(id, authentication);

    final var input = new DeleteUser.Input.Id(new UserId(id));

    executor.execute(deleteUserUsecase, input);
  }

  private void requireSelfOrAdmin(UUID targetUserId, @Nullable Authentication authentication) {
    final var effectiveAuth = resolveAuthentication(authentication);
    if (isAdmin(effectiveAuth)) {
      return;
    }
    final var currentUserId = extractCurrentUserId(effectiveAuth);
    if (!currentUserId.equals(targetUserId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You are not allowed to access or modify another user's account");
    }
  }

  private @Nullable Authentication resolveAuthentication(@Nullable Authentication authentication) {
    if (authentication != null) {
      return authentication;
    }
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private boolean isAdmin(@Nullable Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }

  private UUID extractCurrentUserId(@Nullable Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied", e);
    }
  }

  private @Nullable String extractClientIp(@Nullable HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    final var forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      final var commaIndex = forwardedFor.indexOf(',');
      return (commaIndex > 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
    }
    return request.getRemoteAddr();
  }
}

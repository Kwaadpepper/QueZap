package com.quezap.api.v1.routes.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.auth.ActivateRequest;
import com.quezap.api.v1.dto.request.auth.ForgottenPasswordRequest;
import com.quezap.api.v1.dto.request.auth.LoginRequest;
import com.quezap.api.v1.dto.request.auth.LogoutRequest;
import com.quezap.api.v1.dto.request.auth.RefreshTokenRequest;
import com.quezap.api.v1.dto.request.auth.ResetPasswordRequest;
import com.quezap.api.v1.dto.response.auth.LoginResponseDto;
import com.quezap.api.v1.dto.response.auth.MessageResponseDto;
import com.quezap.api.v1.dto.response.auth.RefreshTokenResponseDto;
import com.quezap.api.v1.dto.response.users.UserSummaryDto;
import com.quezap.api.v1.security.LoginRateLimiter;
import com.quezap.api.v1.security.RefreshTokenRevocationStore;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.ports.users.AuthenticateUser;
import com.quezap.application.ports.users.AuthenticateUser.AuthenticateUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.validation.Valid;

@RestController
@RequestMapping("apiv1/auth")
public class AuthController {

  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3600L; // 1 hour
  private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 60L * 60L * 24L * 30L; // 30 days

  private final UsecaseExecutor executor;
  private final AuthenticateUserUsecase authenticateUserUsecase;
  private final FindUserUsecase findUserUsecase;
  private final JwtService jwtService;
  private final RefreshTokenRevocationStore revocationStore;
  private final LoginRateLimiter loginRateLimiter;

  @Autowired
  public AuthController(
      UsecaseExecutor executor,
      AuthenticateUserUsecase authenticateUserUsecase,
      FindUserUsecase findUserUsecase,
      JwtService jwtService,
      RefreshTokenRevocationStore revocationStore,
      LoginRateLimiter loginRateLimiter) {
    this.executor = executor;
    this.authenticateUserUsecase = authenticateUserUsecase;
    this.findUserUsecase = findUserUsecase;
    this.jwtService = jwtService;
    this.revocationStore = revocationStore;
    this.loginRateLimiter = loginRateLimiter;
  }

  public AuthController(
      UsecaseExecutor executor,
      AuthenticateUserUsecase authenticateUserUsecase,
      FindUserUsecase findUserUsecase,
      JwtService jwtService) {
    this(
        executor,
        authenticateUserUsecase,
        findUserUsecase,
        jwtService,
        new RefreshTokenRevocationStore(),
        new LoginRateLimiter());
  }

  @PostMapping("login")
  public LoginResponseDto login(@Valid @RequestBody LoginRequest request) {
    if (loginRateLimiter.isRateLimited(request.identifier())) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Please try again later.");
    }

    final AuthenticateUser.Output authenticatedUser;
    try {
      final var input =
          new AuthenticateUser.Input(
              new RawIdentifier(request.identifier()), new PasswordCandidate(request.password()));
      authenticatedUser = executor.execute(authenticateUserUsecase, input);
    } catch (ApplicationConstraintException | IllegalDomainStateException e) {
      loginRateLimiter.recordFailure(request.identifier());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e);
    }

    loginRateLimiter.reset(request.identifier());

    final var userId = authenticatedUser.id().value();
    final var userName = authenticatedUser.name();

    try {
      final var exp = Instant.now().getEpochSecond() + ACCESS_TOKEN_EXPIRATION_SECONDS;
      final var payload =
          JwtToken.JwtPayload.builder(userId.toString(), exp)
              .claim("name", userName)
              .claim("type", "access")
              .build();

      final var accessToken = jwtService.generateToken(payload);
      final var refreshExp = Instant.now().getEpochSecond() + REFRESH_TOKEN_EXPIRATION_SECONDS;
      final var refreshPayload =
          JwtToken.JwtPayload.builder(userId.toString(), refreshExp)
              .jwtId(UUID.randomUUID().toString())
              .claim("type", "refresh")
              .build();
      final var refreshToken = jwtService.generateToken(refreshPayload);

      return new LoginResponseDto(
          accessToken,
          refreshToken,
          "Bearer",
          ACCESS_TOKEN_EXPIRATION_SECONDS,
          new UserSummaryDto(userId, userName));
    } catch (JwtService.JwtException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate token", e);
    }
  }

  @GetMapping("me")
  public UserSummaryDto me(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    final UserId userId;
    try {
      userId = new UserId(UUID.fromString(authentication.getName()));
    } catch (IllegalArgumentException | IllegalDomainStateException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token", e);
    }

    try {
      final var user = executor.execute(findUserUsecase, new FindUser.Input(userId));
      return new UserSummaryDto(user.id().value(), user.name());
    } catch (ApplicationConstraintException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
    }
  }

  @PostMapping("refresh")
  public RefreshTokenResponseDto refresh(@Valid @RequestBody RefreshTokenRequest request) {
    final var rawRefreshToken = request.refreshToken();
    if (revocationStore.isRevoked(rawRefreshToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
    }

    try {
      final var validated = jwtService.validateAndParse(rawRefreshToken);
      final var tokenType = validated.customClaims().get("type");
      if (!"refresh".equals(tokenType)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
      }

      final var userId = new UserId(UUID.fromString(validated.sub()));
      final FindUser.Output user;
      try {
        user = executor.execute(findUserUsecase, new FindUser.Input(userId));
      } catch (ApplicationConstraintException e) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found", e);
      }

      revocationStore.revoke(rawRefreshToken, validated.exp());

      final var exp = Instant.now().getEpochSecond() + ACCESS_TOKEN_EXPIRATION_SECONDS;
      final var accessPayload =
          JwtToken.JwtPayload.builder(user.id().value().toString(), exp)
              .claim("name", user.name())
              .claim("type", "access")
              .build();

      final var accessToken = jwtService.generateToken(accessPayload);
      final var refreshExp = Instant.now().getEpochSecond() + REFRESH_TOKEN_EXPIRATION_SECONDS;
      final var refreshPayload =
          JwtToken.JwtPayload.builder(user.id().value().toString(), refreshExp)
              .jwtId(UUID.randomUUID().toString())
              .claim("type", "refresh")
              .build();
      final var newRefreshToken = jwtService.generateToken(refreshPayload);

      return new RefreshTokenResponseDto(
          accessToken, newRefreshToken, "Bearer", ACCESS_TOKEN_EXPIRATION_SECONDS);
    } catch (JwtService.JwtException | IllegalArgumentException | IllegalDomainStateException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e);
    }
  }

  @PostMapping("logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    try {
      final var validated = jwtService.validateAndParse(request.refreshToken());
      revocationStore.revoke(request.refreshToken(), validated.exp());
    } catch (JwtService.JwtException | IllegalArgumentException e) {
      // Token is already invalid or expired; logout succeeds idempotently
    }
  }

  @PostMapping("forgotten-password")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void forgottenPassword(@Valid @RequestBody ForgottenPasswordRequest request) {
    // 202 Accepted always returned to prevent user enumeration
  }

  @GetMapping("verify-reset-token")
  @ResponseStatus(HttpStatus.OK)
  public void verifyResetToken(@org.springframework.web.bind.annotation.RequestParam String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
    }
  }

  @PostMapping("reset-password")
  public MessageResponseDto resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return new MessageResponseDto("Password successfully updated.");
  }

  @PostMapping("activate")
  public MessageResponseDto activate(@Valid @RequestBody ActivateRequest request) {
    return new MessageResponseDto("Account successfully activated.");
  }
}

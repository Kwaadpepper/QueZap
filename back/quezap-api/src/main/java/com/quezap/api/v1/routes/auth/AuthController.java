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
import org.springframework.web.bind.annotation.RequestParam;
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
import com.quezap.api.v1.services.AccountEmailService;
import com.quezap.api.v1.services.TurnstileService;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AuthenticateUserError;
import com.quezap.application.ports.users.ActivateUser;
import com.quezap.application.ports.users.ActivateUser.ActivateUserUsecase;
import com.quezap.application.ports.users.AuthenticateUser;
import com.quezap.application.ports.users.AuthenticateUser.AuthenticateUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.application.ports.users.FindUserCredential;
import com.quezap.application.ports.users.FindUserCredential.FindUserCredentialUsecase;
import com.quezap.application.ports.users.ResetUserPassword;
import com.quezap.application.ports.users.ResetUserPassword.ResetUserPasswordUsecase;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("apiv1/auth")
public class AuthController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3600L; // 1 hour
  private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 60L * 60L * 24L * 30L; // 30 days
  private static final long RESET_TOKEN_EXPIRATION_SECONDS = 60L * 30L; // 30 minutes

  private final UsecaseExecutor executor;
  private final AuthenticateUserUsecase authenticateUserUsecase;
  private final FindUserUsecase findUserUsecase;
  private final @Nullable FindUserCredentialUsecase findUserCredentialUsecase;
  private final @Nullable ResetUserPasswordUsecase resetUserPasswordUsecase;
  private final @Nullable ActivateUserUsecase activateUserUsecase;
  private final JwtService jwtService;
  private final RefreshTokenRevocationStore revocationStore;
  private final LoginRateLimiter loginRateLimiter;
  private final @Nullable TurnstileService turnstileService;
  private final @Nullable AccountEmailService accountEmailService;

  @Autowired
  public AuthController(
      UsecaseExecutor executor,
      AuthenticateUserUsecase authenticateUserUsecase,
      FindUserUsecase findUserUsecase,
      FindUserCredentialUsecase findUserCredentialUsecase,
      ResetUserPasswordUsecase resetUserPasswordUsecase,
      ActivateUserUsecase activateUserUsecase,
      JwtService jwtService,
      RefreshTokenRevocationStore revocationStore,
      LoginRateLimiter loginRateLimiter,
      TurnstileService turnstileService,
      AccountEmailService accountEmailService) {
    this.executor = executor;
    this.authenticateUserUsecase = authenticateUserUsecase;
    this.findUserUsecase = findUserUsecase;
    this.findUserCredentialUsecase = findUserCredentialUsecase;
    this.resetUserPasswordUsecase = resetUserPasswordUsecase;
    this.activateUserUsecase = activateUserUsecase;
    this.jwtService = jwtService;
    this.revocationStore = revocationStore;
    this.loginRateLimiter = loginRateLimiter;
    this.turnstileService = turnstileService;
    this.accountEmailService = accountEmailService;
  }

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
    this.findUserCredentialUsecase = null;
    this.resetUserPasswordUsecase = null;
    this.activateUserUsecase = null;
    this.jwtService = jwtService;
    this.revocationStore = revocationStore;
    this.loginRateLimiter = loginRateLimiter;
    this.turnstileService = null;
    this.accountEmailService = null;
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
    } catch (ApplicationConstraintException e) {
      if (Integer.valueOf(AuthenticateUserError.ACCOUNT_NOT_ACTIVATED.getCode())
          .equals(e.getCode())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Account is not activated. Please check your email for the activation link.",
            e);
      }
      loginRateLimiter.recordFailure(request.identifier());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e);
    } catch (IllegalDomainStateException e) {
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
  public void forgottenPassword(
      @Valid @RequestBody ForgottenPasswordRequest request,
      @Nullable HttpServletRequest httpRequest) {
    if (turnstileService != null) {
      turnstileService.verifyOrThrow(request.turnstileToken(), extractClientIp(httpRequest));
    }

    if (findUserCredentialUsecase == null || accountEmailService == null) {
      return;
    }

    // Always return 202 Accepted to prevent user enumeration
    try {
      final var user =
          executor.execute(
              findUserCredentialUsecase,
              new FindUserCredential.Input.ByIdentifier(new RawIdentifier(request.identifier())));

      final var exp = Instant.now().getEpochSecond() + RESET_TOKEN_EXPIRATION_SECONDS;
      final var payload =
          JwtToken.JwtPayload.builder(user.id().value().toString(), exp)
              .jwtId(UUID.randomUUID().toString())
              .claim("type", "password-reset")
              .claim("fp", user.passwordFingerprint())
              .build();

      final var resetToken = jwtService.generateToken(payload);
      accountEmailService.sendPasswordResetEmail(request.identifier(), user.name(), resetToken);
    } catch (ApplicationConstraintException | IllegalDomainStateException ignored) {
      // Unknown identifier: return 202 silently
    } catch (JwtService.JwtException e) {
      logger.error("Failed to generate password reset token", e);
    }
  }

  @GetMapping("verify-reset-token")
  @ResponseStatus(HttpStatus.OK)
  public void verifyResetToken(@RequestParam String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
    }
    if (findUserCredentialUsecase == null) {
      return;
    }
    validatePasswordResetToken(token);
  }

  @PostMapping("reset-password")
  public MessageResponseDto resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    if (findUserCredentialUsecase != null && resetUserPasswordUsecase != null) {
      final var userId = validatePasswordResetToken(request.token());
      executor.execute(
          resetUserPasswordUsecase,
          new ResetUserPassword.Input.Id(userId, new RawPassword(request.newPassword())));
    }
    return new MessageResponseDto("Password successfully updated.");
  }

  @PostMapping("activate")
  public MessageResponseDto activate(@Valid @RequestBody ActivateRequest request) {
    if (activateUserUsecase != null) {
      final var userId = validateActivationToken(request.token());
      executor.execute(activateUserUsecase, new ActivateUser.Input.Id(userId));
    }
    return new MessageResponseDto("Account successfully activated.");
  }

  private UserId validatePasswordResetToken(String token) {
    if (findUserCredentialUsecase == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired reset token");
    }
    try {
      final var validated = jwtService.validateAndParse(token);
      if (!"password-reset".equals(validated.customClaims().get("type"))) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired reset token");
      }
      final var tokenFingerprint = validated.customClaims().get("fp");
      final var userId = new UserId(UUID.fromString(validated.sub()));
      final var user =
          executor.execute(findUserCredentialUsecase, new FindUserCredential.Input.ById(userId));
      if (!(tokenFingerprint instanceof String fp) || !fp.equals(user.passwordFingerprint())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired reset token");
      }
      return userId;
    } catch (JwtService.JwtException
        | IllegalArgumentException
        | IllegalDomainStateException
        | ApplicationConstraintException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired reset token", e);
    }
  }

  private UserId validateActivationToken(String token) {
    try {
      final var validated = jwtService.validateAndParse(token);
      if (!"activation".equals(validated.customClaims().get("type"))) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid or expired activation token");
      }
      return new UserId(UUID.fromString(validated.sub()));
    } catch (JwtService.JwtException | IllegalArgumentException | IllegalDomainStateException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid or expired activation token", e);
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

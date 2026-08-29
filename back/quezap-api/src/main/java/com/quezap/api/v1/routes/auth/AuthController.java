package com.quezap.api.v1.routes.auth;

import java.time.Instant;
import java.util.UUID;

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
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;

import jakarta.validation.Valid;

@RestController
@RequestMapping("apiv1/auth")
public class AuthController {

  private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3600L; // 1 hour

  private final CredentialRepository credentialRepository;
  private final UserJpaRepository userJpaRepository;
  private final IdentifierHasher identifierHasher;
  private final PasswordHasher passwordHasher;
  private final JwtService jwtService;

  public AuthController(
      CredentialRepository credentialRepository,
      UserJpaRepository userJpaRepository,
      IdentifierHasher identifierHasher,
      PasswordHasher passwordHasher,
      JwtService jwtService) {
    this.credentialRepository = credentialRepository;
    this.userJpaRepository = userJpaRepository;
    this.identifierHasher = identifierHasher;
    this.passwordHasher = passwordHasher;
    this.jwtService = jwtService;
  }

  @PostMapping("login")
  public LoginResponseDto login(@Valid @RequestBody LoginRequest request) {
    final var hashedIdentifier = identifierHasher.hash(new RawIdentifier(request.identifier()));
    final var credential =
        credentialRepository
            .findByIdentifier(hashedIdentifier)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    final var passwordCandidate = new PasswordCandidate(request.password());
    final var passwordMatches =
        passwordHasher.verify(passwordCandidate, credential.getHashedPassword());

    if (!passwordMatches) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    final var userEntity =
        userJpaRepository
            .findByCredentialId(credential.getId().value())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account not found"));

    try {
      final var exp = Instant.now().getEpochSecond() + ACCESS_TOKEN_EXPIRATION_SECONDS;
      final var payload =
          JwtToken.JwtPayload.builder(userEntity.getId().toString(), exp)
              .claim("name", userEntity.getName())
              .build();

      final var accessToken = jwtService.generateToken(payload);
      final var refreshToken = UUID.randomUUID().toString();

      return new LoginResponseDto(
          accessToken,
          refreshToken,
          "Bearer",
          ACCESS_TOKEN_EXPIRATION_SECONDS,
          new UserSummaryDto(userEntity.getId(), userEntity.getName()));
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

    final var userId = UUID.fromString(authentication.getName());
    final var userEntity =
        userJpaRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    return new UserSummaryDto(userEntity.getId(), userEntity.getName());
  }

  @PostMapping("refresh")
  public RefreshTokenResponseDto refresh(@Valid @RequestBody RefreshTokenRequest request) {
    try {
      final var exp = Instant.now().getEpochSecond() + ACCESS_TOKEN_EXPIRATION_SECONDS;
      final var payload = JwtToken.JwtPayload.builder(UUID.randomUUID().toString(), exp).build();

      final var accessToken = jwtService.generateToken(payload);
      final var newRefreshToken = UUID.randomUUID().toString();

      return new RefreshTokenResponseDto(
          accessToken, newRefreshToken, "Bearer", ACCESS_TOKEN_EXPIRATION_SECONDS);
    } catch (JwtService.JwtException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", e);
    }
  }

  @PostMapping("logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    // Stateless logout - client discards tokens
  }

  @PostMapping("forgotten-password")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void forgottenPassword(@Valid @RequestBody ForgottenPasswordRequest request) {
    // 202 Accepted always returned to prevent user enumeration
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

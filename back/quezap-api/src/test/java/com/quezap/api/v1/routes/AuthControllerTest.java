package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.auth.ActivateRequest;
import com.quezap.api.v1.dto.request.auth.LoginRequest;
import com.quezap.api.v1.dto.request.auth.LogoutRequest;
import com.quezap.api.v1.dto.request.auth.RefreshTokenRequest;
import com.quezap.api.v1.dto.request.auth.ResetPasswordRequest;
import com.quezap.api.v1.routes.auth.AuthController;
import com.quezap.api.v1.security.LoginRateLimiter;
import com.quezap.api.v1.security.RefreshTokenRevocationStore;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AuthenticateUserError;
import com.quezap.application.ports.users.AuthenticateUser;
import com.quezap.application.ports.users.AuthenticateUser.AuthenticateUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthControllerTest {

  private UsecaseExecutor executor;
  private AuthenticateUserUsecase authenticateUserUsecase;
  private FindUserUsecase findUserUsecase;
  private JwtService jwtService;
  private RefreshTokenRevocationStore revocationStore;
  private LoginRateLimiter rateLimiter;
  private AuthController authController;

  @BeforeEach
  void setUp() {
    executor = Mockito.mock(UsecaseExecutor.class);
    authenticateUserUsecase = Mockito.mock(AuthenticateUserUsecase.class);
    findUserUsecase = Mockito.mock(FindUserUsecase.class);
    jwtService = Mockito.mock(JwtService.class);
    revocationStore = new RefreshTokenRevocationStore();
    rateLimiter = new LoginRateLimiter(3, 60L);
    authController =
        new AuthController(
            executor,
            authenticateUserUsecase,
            findUserUsecase,
            jwtService,
            revocationStore,
            rateLimiter);
  }

  @Test
  void shouldLoginSuccessfully() throws Exception {
    final var userId = UuidV7.randomUuid();
    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenReturn(new AuthenticateUser.Output(new UserId(userId), "Tournesol"));
    when(jwtService.generateToken(any())).thenReturn("jwt-token-123");

    final var response = authController.login(new LoginRequest("tournesol", "Pass1234!"));

    assertThat(response.accessToken()).isEqualTo("jwt-token-123");
    assertThat(response.user().name()).isEqualTo("Tournesol");
    assertThat(response.tokenType()).isEqualTo("Bearer");
  }

  @Test
  void shouldRejectLoginWhenCredentialsInvalid() {
    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenThrow(new ApplicationConstraintException(AuthenticateUserError.INVALID_CREDENTIALS));

    assertThatThrownBy(() -> authController.login(new LoginRequest("tournesol", "Wrong123!")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldRateLimitLoginAfterRepeatedFailures() {
    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenThrow(new ApplicationConstraintException(AuthenticateUserError.INVALID_CREDENTIALS));

    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> authController.login(new LoginRequest("tournesol", "Wrong123!")))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(e -> ((ResponseStatusException) e).getStatusCode())
          .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    assertThatThrownBy(() -> authController.login(new LoginRequest("tournesol", "Wrong123!")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void shouldRefreshToken() throws Exception {
    final var userId = UuidV7.randomUuid();
    final var now = TimelinePoint.now();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .build();
    when(jwtService.validateAndParse("refresh-token-xyz")).thenReturn(payload);
    when(executor.execute(eq(findUserUsecase), any(FindUser.Input.class)))
        .thenReturn(new FindUser.Output(new UserId(userId), "Tournesol", now, now));
    when(jwtService.generateToken(any())).thenReturn("refreshed-jwt-token");

    final var response = authController.refresh(new RefreshTokenRequest("refresh-token-xyz"));

    assertThat(response.accessToken()).isEqualTo("refreshed-jwt-token");
  }

  @Test
  void shouldRejectRefreshTokenWithoutTypeClaim() throws Exception {
    final var userId = UUID.randomUUID();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .build();
    when(jwtService.validateAndParse("token-without-type")).thenReturn(payload);

    assertThatThrownBy(() -> authController.refresh(new RefreshTokenRequest("token-without-type")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldRejectRefreshTokenWithAccessTypeClaim() throws Exception {
    final var userId = UUID.randomUUID();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "access")
            .build();
    when(jwtService.validateAndParse("access-token")).thenReturn(payload);

    assertThatThrownBy(() -> authController.refresh(new RefreshTokenRequest("access-token")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldRejectRevokedRefreshTokenAfterLogout() throws Exception {
    final var userId = UuidV7.randomUuid();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .build();
    when(jwtService.validateAndParse("refresh-token-to-revoke")).thenReturn(payload);

    authController.logout(new LogoutRequest("refresh-token-to-revoke"));

    assertThatThrownBy(
            () -> authController.refresh(new RefreshTokenRequest("refresh-token-to-revoke")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldReturn401WhenMeHasInvalidUuidSubject() {
    final var auth =
        new UsernamePasswordAuthenticationToken(
            "not-a-uuid", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

    assertThatThrownBy(() -> authController.me(auth))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldResetPassword() {
    final var response =
        authController.resetPassword(new ResetPasswordRequest("token-123", "NewPass456!"));

    assertThat(response.message()).contains("successfully updated");
  }

  @Test
  void shouldActivateAccount() {
    final var response = authController.activate(new ActivateRequest("token-123"));

    assertThat(response.message()).contains("successfully activated");
  }
}

package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.auth.ActivateRequest;
import com.quezap.api.v1.dto.request.auth.ForgottenPasswordRequest;
import com.quezap.api.v1.dto.request.auth.LoginRequest;
import com.quezap.api.v1.dto.request.auth.ResetPasswordRequest;
import com.quezap.api.v1.dto.request.auth.VerifyResetTokenRequest;
import com.quezap.api.v1.routes.auth.AuthController;
import com.quezap.api.v1.security.LoginRateLimiter;
import com.quezap.api.v1.security.RefreshTokenRevocationStore;
import com.quezap.api.v1.services.AccountEmailService;
import com.quezap.api.v1.services.TurnstileService;
import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.application.exceptions.users.AuthenticateUserError;
import com.quezap.application.ports.users.ActivateUser.ActivateUserUsecase;
import com.quezap.application.ports.users.AuthenticateUser;
import com.quezap.application.ports.users.AuthenticateUser.AuthenticateUserUsecase;
import com.quezap.application.ports.users.FindUser;
import com.quezap.application.ports.users.FindUser.FindUserUsecase;
import com.quezap.application.ports.users.FindUserCredential;
import com.quezap.application.ports.users.FindUserCredential.FindUserCredentialUsecase;
import com.quezap.application.ports.users.ResetUserPassword.ResetUserPasswordUsecase;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;
import com.quezap.infrastructure.adapter.services.jwt.JwtToken;
import com.quezap.lib.ddd.usecases.UsecaseExecutor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  void shouldLoginSuccessfullyAndSetHttpOnlyRefreshCookie() throws Exception {
    final var userId = UuidV7.randomUuid();
    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenReturn(new AuthenticateUser.Output(new UserId(userId), "Tournesol"));
    when(jwtService.generateToken(any())).thenReturn("jwt-token-123");

    final var servletResponse = new MockHttpServletResponse();
    final var response =
        authController.login(
            new LoginRequest("tournesol", "Pass1234!"),
            new MockHttpServletRequest(),
            servletResponse);

    assertThat(response.accessToken()).isEqualTo("jwt-token-123");
    assertThat(response.user().name()).isEqualTo("Tournesol");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    final var setCookie = servletResponse.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .isNotNull()
        .contains("quezap_refresh_token=jwt-token-123")
        .contains("HttpOnly")
        .contains("SameSite=Strict")
        .contains("Path=/apiv1/auth");
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
  void shouldRefreshTokenAndRotateCookie() throws Exception {
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

    final var servletResponse = new MockHttpServletResponse();
    final var response = authController.refresh("refresh-token-xyz", servletResponse);

    assertThat(response.accessToken()).isEqualTo("refreshed-jwt-token");
    assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
        .contains("quezap_refresh_token=refreshed-jwt-token")
        .contains("HttpOnly")
        .contains("SameSite=Strict");
  }

  @Test
  void shouldRejectRefreshWhenCookieMissing() {
    assertThatThrownBy(() -> authController.refresh(null, new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldRejectRefreshTokenWithoutTypeClaim() throws Exception {
    final var userId = UUID.randomUUID();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .build();
    when(jwtService.validateAndParse("token-without-type")).thenReturn(payload);

    assertThatThrownBy(
            () -> authController.refresh("token-without-type", new MockHttpServletResponse()))
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

    assertThatThrownBy(() -> authController.refresh("access-token", new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldRejectRevokedRefreshTokenAfterLogoutAndClearCookie() throws Exception {
    final var userId = UuidV7.randomUuid();
    final var payload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .build();
    when(jwtService.validateAndParse("refresh-token-to-revoke")).thenReturn(payload);

    final var logoutResponse = new MockHttpServletResponse();
    authController.logout("refresh-token-to-revoke", logoutResponse);

    assertThat(logoutResponse.getHeader(HttpHeaders.SET_COOKIE))
        .contains("quezap_refresh_token=")
        .contains("Max-Age=0")
        .contains("HttpOnly")
        .contains("SameSite=Strict");

    assertThatThrownBy(
            () -> authController.refresh("refresh-token-to-revoke", new MockHttpServletResponse()))
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

  @Test
  void shouldIncludePasswordFingerprintInRefreshTokenAndRejectStaleFingerprint() throws Exception {
    final var findUserCredentialUsecase = Mockito.mock(FindUserCredentialUsecase.class);
    final var fullController = fullAuthController(findUserCredentialUsecase);
    final var userId = UuidV7.randomUuid();
    final var now = TimelinePoint.now();

    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenReturn(new AuthenticateUser.Output(new UserId(userId), "Tournesol"));
    when(executor.execute(eq(findUserCredentialUsecase), any(FindUserCredential.Input.class)))
        .thenReturn(
            new FindUserCredential.Output(
                new UserId(userId), "Tournesol", true, "fp-v1", now, now));
    when(jwtService.generateToken(any())).thenReturn("generated-jwt");

    final var servletResponse = new MockHttpServletResponse();
    fullController.login(
        new LoginRequest("tournesol", "Pass1234!"), new MockHttpServletRequest(), servletResponse);
    assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE)).contains("Secure");

    final var payloadCaptor = ArgumentCaptor.forClass(JwtToken.JwtPayload.class);
    verify(jwtService, times(2)).generateToken(payloadCaptor.capture());
    final var refreshPayload = payloadCaptor.getAllValues().get(1);
    assertThat(refreshPayload.customClaims().get("type")).isEqualTo("refresh");
    assertThat(refreshPayload.customClaims().get("fp")).isEqualTo("fp-v1");

    final var validRefreshPayload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .claim("fp", "fp-v1")
            .build();
    when(jwtService.validateAndParse("valid-fp-refresh")).thenReturn(validRefreshPayload);
    when(executor.execute(eq(findUserUsecase), any(FindUser.Input.class)))
        .thenReturn(new FindUser.Output(new UserId(userId), "Tournesol", now, now));

    final var refreshed = fullController.refresh("valid-fp-refresh", new MockHttpServletResponse());
    assertThat(refreshed.accessToken()).isEqualTo("generated-jwt");

    // Password changed -> fingerprint is now fp-v2
    when(executor.execute(eq(findUserCredentialUsecase), any(FindUserCredential.Input.class)))
        .thenReturn(
            new FindUserCredential.Output(
                new UserId(userId), "Tournesol", true, "fp-v2", now, now));
    final var staleRefreshPayload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .claim("fp", "fp-v1")
            .build();
    when(jwtService.validateAndParse("stale-fp-refresh")).thenReturn(staleRefreshPayload);

    assertThatThrownBy(
            () -> fullController.refresh("stale-fp-refresh", new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);

    final var missingFpPayload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 3600)
            .claim("type", "refresh")
            .build();
    when(jwtService.validateAndParse("missing-fp-refresh")).thenReturn(missingFpPayload);

    assertThatThrownBy(
            () -> fullController.refresh("missing-fp-refresh", new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldVerifyResetTokenViaPostRequest() throws Exception {
    final var findUserCredentialUsecase = Mockito.mock(FindUserCredentialUsecase.class);
    final var fullController = fullAuthController(findUserCredentialUsecase);
    final var userId = UuidV7.randomUuid();
    final var now = TimelinePoint.now();

    final var resetPayload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 1800)
            .claim("type", "password-reset")
            .claim("fp", "fp-reset")
            .build();
    when(jwtService.validateAndParse("valid-reset-tok")).thenReturn(resetPayload);
    when(executor.execute(eq(findUserCredentialUsecase), any(FindUserCredential.Input.class)))
        .thenReturn(
            new FindUserCredential.Output(
                new UserId(userId), "Tournesol", true, "fp-reset", now, now));

    fullController.verifyResetToken(new VerifyResetTokenRequest("valid-reset-tok"));

    final var wrongTypePayload =
        JwtToken.JwtPayload.builder(userId.toString(), Instant.now().getEpochSecond() + 1800)
            .claim("type", "access")
            .claim("fp", "fp-reset")
            .build();
    when(jwtService.validateAndParse("access-tok")).thenReturn(wrongTypePayload);

    assertThatThrownBy(
            () -> fullController.verifyResetToken(new VerifyResetTokenRequest("access-tok")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldIsolateLoginRateLimitByClientIpAndEnforceForgottenPasswordIpLimit() {
    when(executor.execute(eq(authenticateUserUsecase), any(AuthenticateUser.Input.class)))
        .thenThrow(new ApplicationConstraintException(AuthenticateUserError.INVALID_CREDENTIALS));

    final var attackerReq = new MockHttpServletRequest();
    attackerReq.addHeader("X-Real-IP", "198.51.100.50");
    final var victimReq = new MockHttpServletRequest();
    victimReq.addHeader("X-Real-IP", "203.0.113.10");

    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(
              () -> authController.login(new LoginRequest("tournesol", "Wrong!"), attackerReq))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(e -> ((ResponseStatusException) e).getStatusCode())
          .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    assertThatThrownBy(
            () -> authController.login(new LoginRequest("tournesol", "Wrong!"), attackerReq))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    // Same identifier from another IP is NOT blocked
    assertThatThrownBy(
            () -> authController.login(new LoginRequest("tournesol", "Wrong!"), victimReq))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);

    for (int i = 0; i < 5; i++) {
      authController.forgottenPassword(
          new ForgottenPasswordRequest("tournesol@example.com", null), attackerReq);
    }
    assertThatThrownBy(
            () ->
                authController.forgottenPassword(
                    new ForgottenPasswordRequest("tournesol@example.com", null), attackerReq))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  private AuthController fullAuthController(FindUserCredentialUsecase findUserCredentialUsecase) {
    return new AuthController(
        executor,
        authenticateUserUsecase,
        findUserUsecase,
        findUserCredentialUsecase,
        Mockito.mock(ResetUserPasswordUsecase.class),
        Mockito.mock(ActivateUserUsecase.class),
        jwtService,
        revocationStore,
        rateLimiter,
        Mockito.mock(TurnstileService.class),
        Mockito.mock(AccountEmailService.class),
        true);
  }
}

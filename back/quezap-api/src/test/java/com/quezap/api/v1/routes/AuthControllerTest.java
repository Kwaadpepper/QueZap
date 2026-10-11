package com.quezap.api.v1.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.dto.request.auth.ActivateRequest;
import com.quezap.api.v1.dto.request.auth.LoginRequest;
import com.quezap.api.v1.dto.request.auth.RefreshTokenRequest;
import com.quezap.api.v1.dto.request.auth.ResetPasswordRequest;
import com.quezap.api.v1.routes.auth.AuthController;
import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.domain.ports.services.IdentifierHasher;
import com.quezap.domain.ports.services.PasswordHasher;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.infrastructure.adapter.services.jwt.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthControllerTest {

  private CredentialRepository credentialRepository;
  private UserJpaRepository userJpaRepository;
  private IdentifierHasher identifierHasher;
  private PasswordHasher passwordHasher;
  private JwtService jwtService;
  private AuthController authController;

  @BeforeEach
  void setUp() {
    credentialRepository = Mockito.mock(CredentialRepository.class);
    userJpaRepository = Mockito.mock(UserJpaRepository.class);
    identifierHasher = Mockito.mock(IdentifierHasher.class);
    passwordHasher = Mockito.mock(PasswordHasher.class);
    jwtService = Mockito.mock(JwtService.class);
    authController =
        new AuthController(
            credentialRepository, userJpaRepository, identifierHasher, passwordHasher, jwtService);
  }

  @Test
  void shouldLoginSuccessfully() throws Exception {
    final var userId = UUID.randomUUID();
    final var credId = UUID.randomUUID();
    final var validHash = "$argon2id$v=19$m=16384,t=2,p=1$fakehashedpassword";
    final var credential =
        new Credential(new HashedPassword(validHash), new HashedIdentifier("hashedId"));

    final var credEntity =
        new CredentialJpaEntity(credId, "hashedId", validHash, null, Instant.now(), Instant.now());
    final var userEntity =
        new UserJpaEntity(userId, "Tournesol", credEntity, Instant.now(), Instant.now());

    when(identifierHasher.hash(any())).thenReturn(new HashedIdentifier("hashedId"));
    when(credentialRepository.findByIdentifier(any())).thenReturn(Optional.of(credential));
    when(passwordHasher.verify(any(), any())).thenReturn(true);
    when(userJpaRepository.findByCredentialId(any())).thenReturn(Optional.of(userEntity));
    when(jwtService.generateToken(any())).thenReturn("jwt-token-123");

    final var response = authController.login(new LoginRequest("tournesol", "Pass1234!"));

    assertThat(response.accessToken()).isEqualTo("jwt-token-123");
    assertThat(response.user().name()).isEqualTo("Tournesol");
    assertThat(response.tokenType()).isEqualTo("Bearer");
  }

  @Test
  void shouldRejectLoginWhenCredentialsInvalid() {
    when(identifierHasher.hash(any())).thenReturn(new HashedIdentifier("hashedId"));
    when(credentialRepository.findByIdentifier(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authController.login(new LoginRequest("tournesol", "Wrong123!")))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void shouldRefreshToken() throws Exception {
    final var userId = UUID.randomUUID();
    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    final var userEntity =
        new UserJpaEntity(userId, "Tournesol", cred, Instant.now(), Instant.now());
    final var payload =
        com.quezap.infrastructure.adapter.services.jwt.JwtToken.JwtPayload.builder(
                userId.toString(), Instant.now().getEpochSecond() + 3600)
            .build();
    when(jwtService.validateAndParse("refresh-token-xyz")).thenReturn(payload);
    when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
    when(jwtService.generateToken(any())).thenReturn("refreshed-jwt-token");

    final var response = authController.refresh(new RefreshTokenRequest("refresh-token-xyz"));

    assertThat(response.accessToken()).isEqualTo("refreshed-jwt-token");
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

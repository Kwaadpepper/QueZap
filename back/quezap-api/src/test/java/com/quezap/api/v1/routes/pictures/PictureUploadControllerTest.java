package com.quezap.api.v1.routes.pictures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.api.v1.services.PictureMetadataValidatorService;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.infrastructure.adapter.services.StagedPictureService;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PictureUploadControllerTest {

  private QuestionPictureManager questionPictureManager;
  private StagedPictureService stagedPictureService;
  private PictureMetadataValidatorService validatorService;
  private UserJpaRepository userJpaRepository;
  private PictureUploadController controller;

  private final UUID userId = UUID.randomUUID();
  private UserJpaEntity user;

  @BeforeEach
  void setUp() {
    questionPictureManager = mock(QuestionPictureManager.class);
    stagedPictureService = mock(StagedPictureService.class);
    validatorService = mock(PictureMetadataValidatorService.class);
    userJpaRepository = mock(UserJpaRepository.class);

    controller =
        new PictureUploadController(
            questionPictureManager, stagedPictureService, validatorService, userJpaRepository);

    final var cred =
        new CredentialJpaEntity(
            UUID.randomUUID(), "hid", "hpass", null, Instant.now(), Instant.now());
    user = new UserJpaEntity(userId, "Teacher", cred, Instant.now(), Instant.now());
    when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));

    final var auth = new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldUploadPictureSuccessfully() {
    final var content = "fake-png-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    final var file = new MockMultipartFile("file", "test.png", "image/png", content);

    when(validatorService.validateAndGetMimeType(file)).thenReturn("image/png");

    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var storedPicture = new Picture(objectKey, PictureType.PNG, new Sha256Hash(new byte[32]));
    when(questionPictureManager.store(any())).thenReturn(storedPicture);

    final var expiresAt = Instant.now().plusSeconds(7200);
    final var stagedEntity =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            objectKey,
            "/quezap-public-images/" + objectKey,
            userId,
            Instant.now(),
            expiresAt);
    when(stagedPictureService.registerStagedPicture(any(), any(), any(), any()))
        .thenReturn(stagedEntity);

    final var response = controller.upload(file);

    assertThat(response).isNotNull();
    assertThat(response.pictureUrl()).isEqualTo("/quezap-public-images/" + objectKey);
    assertThat(response.objectKey()).isEqualTo(objectKey);
    assertThat(response.expiresAt()).isEqualTo(expiresAt);

    verify(questionPictureManager).store(any());
    verify(stagedPictureService)
        .registerStagedPicture(objectKey, "/quezap-public-images/" + objectKey, userId, null);
  }

  @Test
  void shouldRejectEmptyFile() {
    final var file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> controller.upload(file))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldRejectUnauthenticatedUser() {
    SecurityContextHolder.clearContext();
    final var file =
        new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            "content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertThatThrownBy(() -> controller.upload(file))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
  }
}

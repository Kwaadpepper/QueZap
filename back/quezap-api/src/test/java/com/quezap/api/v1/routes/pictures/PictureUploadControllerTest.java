package com.quezap.api.v1.routes.pictures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
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
import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.Sha256Hash;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.models.valueobjects.pictures.PictureType;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager.StagedPictureInfo;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PictureUploadControllerTest {

  private QuestionPictureManager questionPictureManager;
  private StagedPictureManager stagedPictureManager;
  private PictureMetadataValidatorService validatorService;
  private UserRepository userRepository;
  private PictureUploadController controller;

  private final UUID userId = UuidV7.randomUuid();

  @BeforeEach
  void setUp() {
    questionPictureManager = mock(QuestionPictureManager.class);
    stagedPictureManager = mock(StagedPictureManager.class);
    validatorService = mock(PictureMetadataValidatorService.class);
    userRepository = mock(UserRepository.class);

    controller =
        new PictureUploadController(
            questionPictureManager, stagedPictureManager, validatorService, userRepository);

    final var user =
        User.hydrate(
            new UserId(userId),
            "Teacher",
            new CredentialId(UuidV7.randomUuid()),
            TimelinePoint.now());
    when(userRepository.find(new UserId(userId))).thenReturn(Optional.of(user));

    final var auth = new UsernamePasswordAuthenticationToken(userId.toString(), "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldUploadPictureSuccessfully() {
    final var content = "fake-png-content".getBytes(StandardCharsets.UTF_8);
    final var file = new MockMultipartFile("file", "test.png", "image/png", content);

    when(validatorService.validateAndGetMimeType(file)).thenReturn("image/png");

    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var publicUrl = "/quezap-public-images/" + objectKey;
    final var storedPicture = new Picture(objectKey, PictureType.PNG, new Sha256Hash(new byte[32]));
    when(questionPictureManager.store(any())).thenReturn(storedPicture);
    when(stagedPictureManager.toPublicUrl(objectKey)).thenReturn(publicUrl);

    final var expiresAt = Instant.now().plusSeconds(7200);
    final var stagedInfo = new StagedPictureInfo(objectKey, publicUrl, userId, expiresAt);
    when(stagedPictureManager.registerStagedPicture(any(), any(), any(), any()))
        .thenReturn(stagedInfo);

    final var response = controller.upload(file);

    assertThat(response).isNotNull();
    assertThat(response.pictureUrl()).isEqualTo(publicUrl);
    assertThat(response.objectKey()).isEqualTo(objectKey);
    assertThat(response.expiresAt()).isEqualTo(expiresAt);

    verify(questionPictureManager).store(any());
    verify(stagedPictureManager).registerStagedPicture(objectKey, publicUrl, userId, null);
  }

  @Test
  void shouldCompensateS3UploadWhenDatabaseRegistrationFails() {
    final var content = "fake-png-content".getBytes(StandardCharsets.UTF_8);
    final var file = new MockMultipartFile("file", "test.png", "image/png", content);

    when(validatorService.validateAndGetMimeType(file)).thenReturn("image/png");

    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var publicUrl = "/quezap-public-images/" + objectKey;
    final var storedPicture = new Picture(objectKey, PictureType.PNG, new Sha256Hash(new byte[32]));
    when(questionPictureManager.store(any())).thenReturn(storedPicture);
    when(stagedPictureManager.toPublicUrl(objectKey)).thenReturn(publicUrl);
    when(stagedPictureManager.registerStagedPicture(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Database unavailable"));

    assertThatThrownBy(() -> controller.upload(file))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database unavailable");

    verify(questionPictureManager).removeByObjectKey(objectKey);
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
    final var bytes = "content".getBytes(StandardCharsets.UTF_8);
    final var file = new MockMultipartFile("file", "test.png", "image/png", bytes);

    assertThatThrownBy(() -> controller.upload(file))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
  }
}

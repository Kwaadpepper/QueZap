package com.quezap.infrastructure.adapter.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.StagedPictureJpaRepository;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StagedPictureServiceTest {

  private StagedPictureJpaRepository repository;
  private QuestionPictureManager pictureManager;
  private StagedPictureService service;

  @BeforeEach
  void setUp() {
    repository = mock(StagedPictureJpaRepository.class);
    pictureManager = mock(QuestionPictureManager.class);
    service = new StagedPictureService(repository, pictureManager);
  }

  @Test
  void shouldRegisterStagedPictureWithDefaultTtl() {
    final var uploaderId = UuidV7.randomUuid();
    final var objectKey = "pictures/questions/uuid.png";
    final var pictureUrl = "/quezap-public-images/" + objectKey;

    when(repository.save(any(StagedPictureJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    final var registered = service.registerStagedPicture(objectKey, pictureUrl, uploaderId, null);

    assertThat(registered).isNotNull();
    assertThat(registered.getObjectKey()).isEqualTo(objectKey);
    assertThat(registered.getPictureUrl()).isEqualTo(pictureUrl);
    assertThat(registered.getUploaderId()).isEqualTo(uploaderId);
    assertThat(registered.getExpiresAt()).isAfter(registered.getCreatedAt());

    verify(repository).save(any(StagedPictureJpaEntity.class));
  }

  @Test
  void shouldClaimPicturesByUrls() {
    final var urls =
        List.of(
            "/quezap-public-images/pictures/questions/pic1.png",
            "/quezap-public-images/pictures/questions/pic2.png");

    service.claimPictures(urls);

    verify(repository).deleteByPictureUrlIn(urls);
  }

  @Test
  void shouldNotCallRepositoryWhenClaimingEmptyList() {
    service.claimPictures(List.of());

    verify(repository, never()).deleteByPictureUrlIn(any());
  }

  @Test
  void shouldPurgeExpiredPicturesAndRemoveFromS3() {
    final var pic1 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            "pictures/questions/exp1.png",
            "/quezap-public-images/pictures/questions/exp1.png",
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofHours(1)));

    final var pic2 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            "pictures/questions/exp2.png",
            "/quezap-public-images/pictures/questions/exp2.png",
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofMinutes(10)));

    final var expiredList = List.of(pic1, pic2);
    when(repository.findByExpiresAtBefore(any(Instant.class))).thenReturn(expiredList);

    final var count = service.purgeExpiredPictures();

    assertThat(count).isEqualTo(2);
    verify(pictureManager).removeByObjectKey("pictures/questions/exp1.png");
    verify(pictureManager).removeByObjectKey("pictures/questions/exp2.png");
    verify(repository).deleteAllInBatch(expiredList);
  }

  @Test
  void shouldContinuePurgeEvenIfS3FailsForOneFile() {
    final var pic1 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            "pictures/questions/fail.png",
            "/quezap-public-images/pictures/questions/fail.png",
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofHours(1)));

    final var pic2 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            "pictures/questions/ok.png",
            "/quezap-public-images/pictures/questions/ok.png",
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofMinutes(10)));

    final var expiredList = List.of(pic1, pic2);
    when(repository.findByExpiresAtBefore(any(Instant.class))).thenReturn(expiredList);
    doThrow(new RuntimeException("S3 connection error"))
        .when(pictureManager)
        .removeByObjectKey("pictures/questions/fail.png");

    final var count = service.purgeExpiredPictures();

    assertThat(count).isEqualTo(1);
    verify(pictureManager).removeByObjectKey("pictures/questions/fail.png");
    verify(pictureManager).removeByObjectKey("pictures/questions/ok.png");
    verify(repository).deleteAllInBatch(expiredList);
  }
}

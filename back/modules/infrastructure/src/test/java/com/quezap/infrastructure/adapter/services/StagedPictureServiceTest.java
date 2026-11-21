package com.quezap.infrastructure.adapter.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager.InvalidPictureReferenceException;
import com.quezap.domain.ports.services.StagedPictureManager.PictureOwnershipException;
import com.quezap.domain.ports.services.StagedPictureManager.StagedPictureQuotaExceededException;
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
    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pictureUrl = "/quezap-public-images/" + objectKey;

    when(repository.save(any(StagedPictureJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    final var registered = service.registerStagedPicture(objectKey, pictureUrl, uploaderId, null);

    assertThat(registered).isNotNull();
    assertThat(registered.objectKey()).isEqualTo(objectKey);
    assertThat(registered.pictureUrl()).isEqualTo(pictureUrl);
    assertThat(registered.uploaderId()).isEqualTo(uploaderId);
    assertThat(registered.expiresAt()).isAfter(Instant.now());

    verify(repository).save(any(StagedPictureJpaEntity.class));
  }

  @Test
  void shouldClaimPicturesByUrlsForMatchingUploader() {
    final var uploaderId = UuidV7.randomUuid();
    final var url1 = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var url2 = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".webp";
    final var urls = List.of(url1, url2);

    final var now = Instant.now();
    when(repository.findByPictureUrlIn(urls))
        .thenReturn(
            List.of(
                new StagedPictureJpaEntity(
                    UuidV7.randomUuid(),
                    "pictures/questions/1.png",
                    url1,
                    uploaderId,
                    now,
                    now.plusSeconds(3600)),
                new StagedPictureJpaEntity(
                    UuidV7.randomUuid(),
                    "pictures/questions/2.webp",
                    url2,
                    uploaderId,
                    now,
                    now.plusSeconds(3600))));

    service.claimPictures(urls, uploaderId);

    verify(repository).deleteByPictureUrlInAndUploaderId(urls, uploaderId);
  }

  @Test
  void shouldRejectClaimWhenPictureBelongsToAnotherUploader() {
    final var ownerId = UuidV7.randomUuid();
    final var attackerId = UuidV7.randomUuid();
    final var url = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";

    final var now = Instant.now();
    when(repository.findByPictureUrlIn(List.of(url)))
        .thenReturn(
            List.of(
                new StagedPictureJpaEntity(
                    UuidV7.randomUuid(),
                    "pictures/questions/1.png",
                    url,
                    ownerId,
                    now,
                    now.plusSeconds(3600))));

    assertThatThrownBy(() -> service.claimPictures(List.of(url), attackerId))
        .isInstanceOf(PictureOwnershipException.class);

    verify(repository, never()).deleteByPictureUrlInAndUploaderId(any(), any());
  }

  @Test
  void shouldRejectClaimWhenPictureIsNotStaged() {
    final var uploaderId = UuidV7.randomUuid();
    final var url = "/quezap-public-images/pictures/questions/" + UuidV7.randomUuid() + ".png";

    when(repository.findByPictureUrlIn(List.of(url))).thenReturn(List.of());

    assertThatThrownBy(() -> service.claimPictures(List.of(url), uploaderId))
        .isInstanceOf(InvalidPictureReferenceException.class);

    verify(repository, never()).deleteByPictureUrlInAndUploaderId(any(), any());
  }

  @Test
  void shouldNotCallRepositoryWhenClaimingEmptyList() {
    service.claimPictures(List.of(), UuidV7.randomUuid());

    verify(repository, never()).findByPictureUrlIn(any());
    verify(repository, never()).deleteByPictureUrlInAndUploaderId(any(), any());
  }

  @Test
  void shouldPurgeExpiredPicturesAndRemoveFromS3() {
    final var key1 = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var key2 = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pic1 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            key1,
            "/quezap-public-images/" + key1,
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofHours(1)));

    final var pic2 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            key2,
            "/quezap-public-images/" + key2,
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofMinutes(10)));

    final var expiredList = List.of(pic1, pic2);
    when(repository.findByExpiresAtBefore(any(Instant.class))).thenReturn(expiredList);

    final var count = service.purgeExpiredPictures();

    assertThat(count).isEqualTo(2);
    verify(pictureManager).removeByObjectKey(key1);
    verify(pictureManager).removeByObjectKey(key2);
    verify(repository).deleteAllInBatch(expiredList);
  }

  @Test
  void shouldDeleteOnlySuccessfullyPurgedEntriesFromDbWhenS3FailsForOneFile() {
    final var failKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var okKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pic1 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            failKey,
            "/quezap-public-images/" + failKey,
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofHours(1)));

    final var pic2 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            okKey,
            "/quezap-public-images/" + okKey,
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofMinutes(10)));

    final var expiredList = List.of(pic1, pic2);
    when(repository.findByExpiresAtBefore(any(Instant.class))).thenReturn(expiredList);
    doThrow(new RuntimeException("S3 connection error"))
        .when(pictureManager)
        .removeByObjectKey(failKey);

    final var count = service.purgeExpiredPictures();

    assertThat(count).isEqualTo(1);
    verify(pictureManager).removeByObjectKey(failKey);
    verify(pictureManager).removeByObjectKey(okKey);
    verify(repository).deleteAllInBatch(List.of(pic2));
  }

  @Test
  void shouldNotDeleteAnyDbEntryWhenAllS3DeletionsFail() {
    final var failKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pic1 =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(),
            failKey,
            "/quezap-public-images/" + failKey,
            UUID.randomUUID(),
            Instant.now().minus(Duration.ofHours(3)),
            Instant.now().minus(Duration.ofHours(1)));

    when(repository.findByExpiresAtBefore(any(Instant.class))).thenReturn(List.of(pic1));
    doThrow(new RuntimeException("S3 down")).when(pictureManager).removeByObjectKey(failKey);

    final var count = service.purgeExpiredPictures();

    assertThat(count).isEqualTo(0);
    verify(repository, never()).deleteAllInBatch(any());
  }

  @Test
  void shouldValidateAndNormalizeInternalPictureUrlsStrictly() {
    final var uuid = UuidV7.randomUuid();
    final var expected = "/quezap-public-images/pictures/questions/" + uuid + ".png";

    assertThat(service.resolveValidPublicPictureUrl(expected)).isEqualTo(expected);
    assertThat(service.resolveValidPublicPictureUrl("pictures/questions/" + uuid + ".png"))
        .isEqualTo(expected);
    assertThat(service.resolveValidPublicPictureUrl(null)).isNull();
    assertThat(service.resolveValidPublicPictureUrl("   ")).isNull();

    assertThatThrownBy(() -> service.resolveValidPublicPictureUrl("http://evil.com/a.png"))
        .isInstanceOf(InvalidPictureReferenceException.class);
    assertThatThrownBy(
            () ->
                service.resolveValidPublicPictureUrl(
                    "https://evil.com/pictures/questions/" + uuid + ".png"))
        .isInstanceOf(InvalidPictureReferenceException.class);
    assertThatThrownBy(() -> service.resolveValidPublicPictureUrl("data:image/png;base64,AAAA"))
        .isInstanceOf(InvalidPictureReferenceException.class);
    assertThatThrownBy(
            () ->
                service.resolveValidPublicPictureUrl(
                    "/quezap-public-images/pictures/questions/../" + uuid + ".png"))
        .isInstanceOf(InvalidPictureReferenceException.class);
    assertThatThrownBy(() -> service.resolveValidPublicPictureUrl("/apiv1/auth/logout"))
        .isInstanceOf(InvalidPictureReferenceException.class);
  }

  @Test
  void shouldRejectRegisteringWhenActiveStagedQuotaIsReached() {
    final var uploaderId = UuidV7.randomUuid();
    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pictureUrl = "/quezap-public-images/" + objectKey;

    when(repository.countByUploaderIdAndExpiresAtAfter(
            org.mockito.ArgumentMatchers.eq(uploaderId), any(Instant.class)))
        .thenReturn((long) StagedPictureManager.MAX_ACTIVE_STAGED_PICTURES_PER_USER);

    assertThatThrownBy(() -> service.registerStagedPicture(objectKey, pictureUrl, uploaderId, null))
        .isInstanceOf(StagedPictureQuotaExceededException.class)
        .hasMessageContaining("quota exceeded");

    verify(repository, never()).save(any());
  }

  @Test
  void shouldAllowRegisteringWhenActiveStagedCountIsBelowQuota() {
    final var uploaderId = UuidV7.randomUuid();
    final var objectKey = "pictures/questions/" + UuidV7.randomUuid() + ".png";
    final var pictureUrl = "/quezap-public-images/" + objectKey;

    when(repository.countByUploaderIdAndExpiresAtAfter(
            org.mockito.ArgumentMatchers.eq(uploaderId), any(Instant.class)))
        .thenReturn((long) StagedPictureManager.MAX_ACTIVE_STAGED_PICTURES_PER_USER - 1);
    when(repository.save(any(StagedPictureJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    final var registered = service.registerStagedPicture(objectKey, pictureUrl, uploaderId, null);

    assertThat(registered.objectKey()).isEqualTo(objectKey);
    verify(repository).save(any(StagedPictureJpaEntity.class));
  }
}

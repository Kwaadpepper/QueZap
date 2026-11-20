package com.quezap.infrastructure.adapter.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.StagedPictureJpaRepository;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StagedPictureService implements StagedPictureManager {
  private static final Logger logger = LoggerFactory.getLogger(StagedPictureService.class);
  public static final Duration DEFAULT_TTL = Duration.ofHours(2);

  private final StagedPictureJpaRepository stagedPictureJpaRepository;
  private final QuestionPictureManager questionPictureManager;

  public StagedPictureService(
      StagedPictureJpaRepository stagedPictureJpaRepository,
      QuestionPictureManager questionPictureManager) {
    this.stagedPictureJpaRepository = stagedPictureJpaRepository;
    this.questionPictureManager = questionPictureManager;
  }

  @Override
  @Transactional
  public StagedPictureInfo registerStagedPicture(
      String objectKey, String pictureUrl, UUID uploaderId, @Nullable Duration ttl) {
    final var now = Instant.now();
    final var effectiveTtl = Objects.requireNonNullElse(ttl, DEFAULT_TTL);
    final var expiresAt = now.plus(effectiveTtl);

    final var entity =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(), objectKey, pictureUrl, uploaderId, now, expiresAt);

    final var saved = stagedPictureJpaRepository.save(entity);
    final var effective = saved != null ? saved : entity;
    logger.debug(
        "Registered staged picture with key {} and url {}, expires at {}",
        objectKey,
        pictureUrl,
        expiresAt);
    return new StagedPictureInfo(
        effective.getObjectKey(),
        effective.getPictureUrl(),
        effective.getUploaderId(),
        effective.getExpiresAt());
  }

  @Override
  @Transactional
  public void claimPictures(Collection<String> pictureUrls, UUID uploaderId) {
    if (pictureUrls == null || pictureUrls.isEmpty()) {
      return;
    }

    final var nonBlankUrls =
        pictureUrls.stream()
            .filter(u -> u != null && !u.isBlank())
            .map(String::trim)
            .distinct()
            .toList();

    if (nonBlankUrls.isEmpty()) {
      return;
    }

    final var stagedEntries = stagedPictureJpaRepository.findByPictureUrlIn(nonBlankUrls);
    final var stagedByUrl = new HashMap<String, StagedPictureJpaEntity>();
    for (final var entry : stagedEntries) {
      stagedByUrl.put(entry.getPictureUrl(), entry);
    }

    for (final var url : nonBlankUrls) {
      final var entry = stagedByUrl.get(url);
      if (entry == null) {
        throw new InvalidPictureReferenceException("Picture is not staged or has expired: " + url);
      }
      if (!entry.getUploaderId().equals(uploaderId)) {
        throw new PictureOwnershipException(
            "Picture belongs to another user and cannot be claimed: " + url);
      }
    }

    stagedPictureJpaRepository.deleteByPictureUrlInAndUploaderId(nonBlankUrls, uploaderId);
    logger.debug(
        "Claimed and unstaged {} picture(s) for uploader {}", nonBlankUrls.size(), uploaderId);
  }

  @Override
  @Transactional
  public int purgeExpiredPictures() {
    final var now = Instant.now();
    final var expired = stagedPictureJpaRepository.findByExpiresAtBefore(now);

    if (expired.isEmpty()) {
      logger.debug("No expired staged pictures to purge at {}", now);
      return 0;
    }

    final var successfullyDeletedFromS3 = new ArrayList<StagedPictureJpaEntity>();
    for (final var picture : expired) {
      try {
        questionPictureManager.removeByObjectKey(picture.getObjectKey());
        successfullyDeletedFromS3.add(picture);
      } catch (Exception e) {
        logger.warn("Failed to delete expired picture file from S3: {}", picture.getObjectKey(), e);
      }
    }

    if (!successfullyDeletedFromS3.isEmpty()) {
      stagedPictureJpaRepository.deleteAllInBatch(successfullyDeletedFromS3);
    }

    logger.info(
        "Purged {} expired staged pictures from S3 and database ({} failed)",
        successfullyDeletedFromS3.size(),
        expired.size() - successfullyDeletedFromS3.size());
    return successfullyDeletedFromS3.size();
  }

  @Scheduled(cron = "${app.pictures.cleanup-cron:0 */15 * * * *}")
  public void scheduledPurge() {
    try {
      final var count = purgeExpiredPictures();
      if (count > 0) {
        logger.info("Scheduled staged pictures cleanup completed. Removed: {}", count);
      }
    } catch (Exception e) {
      logger.error("Error during scheduled staged pictures cleanup", e);
    }
  }
}

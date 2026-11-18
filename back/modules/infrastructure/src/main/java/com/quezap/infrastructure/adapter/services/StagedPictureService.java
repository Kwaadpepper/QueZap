package com.quezap.infrastructure.adapter.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.StagedPictureJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.StagedPictureJpaRepository;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StagedPictureService {
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

  @Transactional
  public StagedPictureJpaEntity registerStagedPicture(
      String objectKey, String pictureUrl, UUID uploaderId, @Nullable Duration ttl) {
    final var now = Instant.now();
    final var effectiveTtl = Objects.requireNonNullElse(ttl, DEFAULT_TTL);
    final var expiresAt = now.plus(effectiveTtl);

    final var entity =
        new StagedPictureJpaEntity(
            UuidV7.randomUuid(), objectKey, pictureUrl, uploaderId, now, expiresAt);

    final var saved = stagedPictureJpaRepository.save(entity);
    logger.debug(
        "Registered staged picture with key {} and url {}, expires at {}",
        objectKey,
        pictureUrl,
        expiresAt);
    return saved;
  }

  @Transactional
  public void claimPictures(Collection<String> pictureUrls) {
    if (pictureUrls == null || pictureUrls.isEmpty()) {
      return;
    }

    final var nonBlankUrls =
        pictureUrls.stream().filter(u -> u != null && !u.isBlank()).map(String::trim).toList();

    if (nonBlankUrls.isEmpty()) {
      return;
    }

    stagedPictureJpaRepository.deleteByPictureUrlIn(nonBlankUrls);
    logger.debug("Claimed and unstaged {} picture(s)", nonBlankUrls.size());
  }

  @Transactional
  public int purgeExpiredPictures() {
    final var now = Instant.now();
    final var expired = stagedPictureJpaRepository.findByExpiresAtBefore(now);

    if (expired.isEmpty()) {
      logger.debug("No expired staged pictures to purge at {}", now);
      return 0;
    }

    var deletedCount = 0;
    for (final var picture : expired) {
      try {
        questionPictureManager.removeByObjectKey(picture.getObjectKey());
        deletedCount++;
      } catch (Exception e) {
        logger.warn("Failed to delete expired picture file from S3: {}", picture.getObjectKey(), e);
      }
    }

    stagedPictureJpaRepository.deleteAllInBatch(expired);
    logger.info("Purged {} expired staged pictures from S3 and database", deletedCount);
    return deletedCount;
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

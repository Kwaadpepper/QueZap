package com.quezap.application.event.listeners;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.quezap.application.ports.quezaps.QuezapDataPort;
import com.quezap.domain.events.questions.QuestionDeleted;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.domain.ports.services.StagedPictureManager;
import com.quezap.lib.ddd.events.DomainEventListener;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class QuestionRemovedListener implements DomainEventListener<QuestionDeleted> {
  private static final Logger logger = LoggerFactory.getLogger(QuestionRemovedListener.class);
  private final QuestionPictureManager questionPictureManager;
  private final @Nullable QuezapDataPort quezapDataPort;

  public QuestionRemovedListener(QuestionPictureManager questionPictureManager) {
    this(questionPictureManager, null);
  }

  @Autowired
  public QuestionRemovedListener(
      QuestionPictureManager questionPictureManager, @Nullable QuezapDataPort quezapDataPort) {
    this.questionPictureManager = questionPictureManager;
    this.quezapDataPort = quezapDataPort;
  }

  @Override
  @EventListener
  public void onEvent(QuestionDeleted event) {
    event.payload().pictures().forEach(this::removePicture);
  }

  private void removePicture(Picture picture) {
    try {
      if (quezapDataPort != null
          && quezapDataPort.isPictureReferenced(buildReferenceCandidates(picture))) {
        logger.debug(
            "Skipping S3 deletion for picture {} because it is still referenced",
            picture.objectKey());
        return;
      }

      questionPictureManager.remove(picture);

      logger.debug("Successfully removed picture with key {}", picture.objectKey());
    } catch (Exception e) {
      logger.error(
          "Failed to remove picture with key {} for question deletion", picture.objectKey(), e);
    }
  }

  private Set<String> buildReferenceCandidates(Picture picture) {
    final var candidates = new HashSet<String>();
    final var objectKey = picture.objectKey();
    candidates.add(objectKey);
    candidates.add(StagedPictureManager.PUBLIC_BUCKET_PREFIX + objectKey);
    final var normalized = StagedPictureManager.normalizePublicUrl(objectKey);
    if (normalized != null) {
      candidates.add(normalized);
    }
    return candidates;
  }
}

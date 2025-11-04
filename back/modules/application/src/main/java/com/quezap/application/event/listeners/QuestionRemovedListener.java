package com.quezap.application.event.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.quezap.domain.events.questions.QuestionDeleted;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.domain.ports.services.QuestionPictureManager;
import com.quezap.lib.ddd.events.DomainEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class QuestionRemovedListener implements DomainEventListener<QuestionDeleted> {
  private static final Logger logger = LoggerFactory.getLogger(QuestionRemovedListener.class);
  private final QuestionPictureManager questionPictureManager;

  public QuestionRemovedListener(QuestionPictureManager questionPictureManager) {
    this.questionPictureManager = questionPictureManager;
  }

  @Override
  @EventListener
  public void onEvent(QuestionDeleted event) {
    event.payload().pictures().forEach(this::removePicture);
  }

  private void removePicture(Picture picture) {
    try {
      questionPictureManager.remove(picture);

      logger.debug("Successfully removed picture with key {}", picture.objectKey());
    } catch (Exception e) {
      logger.error(
          "Failed to remove picture with key {} for question deletion", picture.objectKey(), e);
    }
  }
}

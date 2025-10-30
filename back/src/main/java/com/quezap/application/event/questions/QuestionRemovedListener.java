package com.quezap.application.event.questions;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.quezap.domain.events.questions.QuestionDeleted;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.ddd.events.DomainEventListener;

@Component
public class QuestionRemovedListener implements DomainEventListener<QuestionDeleted> {
  private final QuestionPictureManager questionPictureManager;

  public QuestionRemovedListener(QuestionPictureManager questionPictureManager) {
    this.questionPictureManager = questionPictureManager;
  }

  @Override
  @EventListener
  public void onEvent(QuestionDeleted event) {
    event.payload().pictures().forEach(questionPictureManager::remove);
  }
}

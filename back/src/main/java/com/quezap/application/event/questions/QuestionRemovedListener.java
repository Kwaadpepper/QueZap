package com.quezap.application.event.questions;

import com.quezap.domain.events.questions.QuestionDeleted;
import com.quezap.domain.port.services.QuestionPictureManager;
import com.quezap.lib.ddd.events.DomainEventListener;

public class QuestionRemovedListener implements DomainEventListener<QuestionDeleted> {
  private final QuestionPictureManager questionPictureManager;

  public QuestionRemovedListener(QuestionPictureManager questionPictureManager) {
    this.questionPictureManager = questionPictureManager;
  }

  @Override
  public void onEvent(QuestionDeleted event) {
    event.payload().pictures().forEach(questionPictureManager::remove);
  }
}

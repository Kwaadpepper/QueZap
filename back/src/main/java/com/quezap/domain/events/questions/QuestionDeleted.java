package com.quezap.domain.events.questions;

import java.time.Instant;
import java.util.Set;

import com.quezap.domain.events.questions.QuestionDeleted.DeletedQuestionPictures;
import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.lib.ddd.events.DomainEvent;

public final class QuestionDeleted implements DomainEvent<DeletedQuestionPictures> {
  private final Set<Picture> pictures;
  private final Instant timestamp;

  public QuestionDeleted(Set<Picture> pictures) {
    this.pictures = pictures;
    this.timestamp = Instant.now();
  }

  @Override
  public String routingKey() {
    return "question.deleted";
  }

  @Override
  public DeletedQuestionPictures payload() {
    return new DeletedQuestionPictures(Set.copyOf(pictures));
  }

  @Override
  public Instant timestamp() {
    return timestamp;
  }

  public static record DeletedQuestionPictures(Set<Picture> pictures) {}
}

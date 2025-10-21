package com.quezap.domain.models.valueobjects.questions;

import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.lib.utils.Domain;

public record QuestionAnswer(ParticipantName participant, Integer slideIndex, Integer answerIndex) {
  public QuestionAnswer {
    Domain.checkDomain(() -> slideIndex >= 0, "Slide index cannot be negative");
    Domain.checkDomain(() -> answerIndex >= 0, "Answer index cannot be negative");
  }
}

package com.quezap.domain.models.valueobjects.questions;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.lib.utils.Domain;

public record QuestionSlide(Integer timer, Integer points, QuestionId question) {
  public QuestionSlide {
    Domain.checkDomain(() -> timer > 0, "A timer must be greater than 0");
    Domain.checkDomain(() -> points > 0, "Points must be greater than 0");
  }
}

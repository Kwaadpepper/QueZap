package com.quezap.domain.models.valueobjects;

import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public record Answer(
    @Nullable String stringAnswer, @Nullable Picture pictureAnswer, Boolean isCorrect) {
  public Answer {
    Domain.checkDomain(
        () ->
            (stringAnswer == null && pictureAnswer != null)
                || (stringAnswer != null && pictureAnswer == null),
        "An answer must have either a string answer or a picture answer");
    if (stringAnswer != null) {
      Domain.checkDomain(
          () -> !stringAnswer.isBlank(), "String answer cannot be blank when it is provided");
      Domain.checkDomain(
          () -> stringAnswer.length() <= 255,
          "String answer cannot exceed 255 characters when it is provided");
    }
  }
}

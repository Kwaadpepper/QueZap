package com.quezap.domain.models.valueobjects;

import com.quezap.domain.models.valueobjects.pictures.Picture;
import com.quezap.lib.utils.Domain;

import org.jspecify.annotations.Nullable;

public record Answer(@Nullable String value, @Nullable Picture picture, Boolean isCorrect) {
  public Answer {
    Domain.checkDomain(
        () -> (value == null && picture != null) || (value != null && picture == null),
        "An answer must have either a string answer or a picture answer");
    if (value != null) {
      Domain.checkDomain(
          () -> !value.isBlank(), "String answer cannot be blank when it is provided");
      Domain.checkDomain(
          () -> value.length() <= 255,
          "String answer cannot exceed 255 characters when it is provided");
    }
  }
}

package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateQuezapRequest(
    @NotBlank(message = "Title cannot be blank") @Size(max = 100, message = "Title cannot exceed 100 characters") String title,
    @Nullable String description,
    @Nullable Boolean isPublic,
    @Nullable List<@Valid QuezapSlideRequest> slides,
    @Nullable List<@Valid QuezapSlideRequest> questions) {

  public CreateQuezapRequest(
      String title,
      @Nullable String description,
      @Nullable Boolean isPublic,
      List<@Valid QuezapSlideRequest> slides) {
    this(title, description, isPublic, slides, slides);
  }

  public List<QuezapSlideRequest> getEffectiveSlides() {
    if (slides != null && !slides.isEmpty()) {
      return slides;
    }
    if (questions != null && !questions.isEmpty()) {
      return questions;
    }
    return List.of();
  }
}

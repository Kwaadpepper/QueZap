package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateQuezapRequest(
    @NotBlank(message = "Title cannot be blank") @Size(max = 100, message = TITLE_MAX) String title,
    @Nullable @Size(max = 2000, message = DESCRIPTION_MAX) String description,
    @Nullable Boolean isPublic,
    @Nullable @Size(max = 100, message = SLIDES_MAX) List<@Valid QuezapSlideRequest> slides,
    @Nullable @Size(max = 100, message = SLIDES_MAX) List<@Valid QuezapSlideRequest> questions) {
  static final String TITLE_MAX = "Title cannot exceed 100 characters";
  static final String DESCRIPTION_MAX = "Description cannot exceed 2000 characters";
  static final String SLIDES_MAX = "A quezap cannot exceed 100 slides";

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

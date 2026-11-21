package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateQuezapRequest(
    @NotBlank(message = "Title cannot be blank") @Size(max = 100, message = TITLE_MAX) String title,
    @Nullable @Size(max = 2000, message = DESCRIPTION_MAX) String description,
    @NotNull(message = "isPublic must be specified") Boolean isPublic,
    @Nullable @Size(max = 100, message = SLIDES_MAX) List<@Valid QuezapSlideRequest> slides) {
  static final String TITLE_MAX = "Title cannot exceed 100 characters";
  static final String DESCRIPTION_MAX = "Description cannot exceed 2000 characters";
  static final String SLIDES_MAX = "A quezap cannot exceed 100 slides";

  public UpdateQuezapRequest(String title, @Nullable String description, Boolean isPublic) {
    this(title, description, isPublic, null);
  }
}

package com.quezap.api.v1.dto.request.quezaps;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateQuezapRequest(
    @NotBlank(message = "Title cannot be blank") @Size(max = 100, message = "Title cannot exceed 100 characters") String title,
    @Nullable String description,
    @NotNull(message = "isPublic must be specified") Boolean isPublic,
    @Nullable List<@Valid QuezapSlideRequest> slides) {

  public UpdateQuezapRequest(String title, @Nullable String description, Boolean isPublic) {
    this(title, description, isPublic, null);
  }
}

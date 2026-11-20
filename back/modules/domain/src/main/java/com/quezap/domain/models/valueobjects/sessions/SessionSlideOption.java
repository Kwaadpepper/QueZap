package com.quezap.domain.models.valueobjects.sessions;

import org.jspecify.annotations.Nullable;

public record SessionSlideOption(
    int index, @Nullable String label, @Nullable String pictureFilename, boolean isCorrect) {

  public boolean hasContent() {
    final var hasLabel = label != null && !label.isBlank();
    final var hasPicture = pictureFilename != null && !pictureFilename.isBlank();
    return hasLabel || hasPicture;
  }
}

package com.quezap.domain.models.valueobjects.sessions;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record SessionSlideSpec(
    int slideOrder,
    UUID questionId,
    @Nullable String type,
    @Nullable String label,
    @Nullable String pictureFilename,
    @Nullable Integer timerSeconds,
    @Nullable Integer points,
    List<SessionSlideOption> options) {

  public SessionSlideSpec {
    options = List.copyOf(options);
  }

  public int effectiveTimerSeconds() {
    return timerSeconds != null ? timerSeconds : 30;
  }

  public int effectivePoints() {
    return points != null ? points : 1000;
  }

  public boolean isBooleanQuestion() {
    return "BOOLEAN".equalsIgnoreCase(type) || "AFFIRMATION".equalsIgnoreCase(type);
  }

  public boolean isCashQuestion() {
    return "CASH".equalsIgnoreCase(type);
  }

  public String normalizedType() {
    if (type == null || type.isBlank()) {
      return "QUIZZ";
    }
    if ("AFFIRMATION".equalsIgnoreCase(type)) {
      return "BOOLEAN";
    }
    return type.toUpperCase(Locale.ROOT);
  }
}

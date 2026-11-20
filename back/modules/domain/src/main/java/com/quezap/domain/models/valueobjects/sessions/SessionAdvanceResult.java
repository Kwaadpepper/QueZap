package com.quezap.domain.models.valueobjects.sessions;

public record SessionAdvanceResult(
    boolean ended, boolean wasPaused, int currentSlideIndex, int totalSlides) {}

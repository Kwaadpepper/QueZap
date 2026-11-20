package com.quezap.domain.models.valueobjects.sessions;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record SessionParticipant(
    UUID id,
    String name,
    int score,
    String participationToken,
    boolean isKicked,
    Instant joinedAt,
    long totalResponseTimeMs,
    int lastAnsweredSlideIndex,
    long lastSlideResponseTimeMs) {

  /**
   * Deterministic ranking comparator: highest score first, then lowest cumulative response time,
   * then earliest arrival time, then canonical participant name.
   */
  public static final Comparator<SessionParticipant> RANKING_COMPARATOR =
      Comparator.comparingInt(SessionParticipant::score)
          .reversed()
          .thenComparingLong(SessionParticipant::totalResponseTimeMs)
          .thenComparing(SessionParticipant::joinedAt)
          .thenComparing(SessionParticipant::name, String.CASE_INSENSITIVE_ORDER)
          .thenComparing(SessionParticipant::name);

  public SessionParticipant withKicked(boolean kicked) {
    return new SessionParticipant(
        id,
        name,
        score,
        participationToken,
        kicked,
        joinedAt,
        totalResponseTimeMs,
        lastAnsweredSlideIndex,
        lastSlideResponseTimeMs);
  }

  public SessionParticipant withUpdatedAnswerTimingAndScore(
      int newScore, int slideIndex, long elapsedMillis) {
    final long updatedTotalTime;
    if (slideIndex == lastAnsweredSlideIndex) {
      updatedTotalTime =
          Math.max(0L, totalResponseTimeMs - lastSlideResponseTimeMs) + elapsedMillis;
    } else {
      updatedTotalTime = totalResponseTimeMs + elapsedMillis;
    }
    return new SessionParticipant(
        id,
        name,
        Math.max(0, newScore),
        participationToken,
        isKicked,
        joinedAt,
        updatedTotalTime,
        slideIndex,
        elapsedMillis);
  }
}

package com.quezap.domain.models.valueobjects.sessions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Test;

class SessionDomainTest {

  private static SessionSlideSpec sampleQuizzSlide(int order, int timerSeconds, int points) {
    return new SessionSlideSpec(
        order,
        UuidV7.randomUuid(),
        "QUIZZ",
        "Question " + order,
        null,
        timerSeconds,
        points,
        List.of(
            new SessionSlideOption(0, "Good", null, true),
            new SessionSlideOption(1, "Bad", null, false)));
  }

  @Test
  void shouldStartSessionWithZeroParticipantsAndAdvanceToEnd() {
    final var now = Instant.parse("2026-11-18T10:00:00Z");
    final var session =
        Session.createLive(
            new SessionId(UuidV7.randomUuid()),
            "Demo Classroom",
            new SessionNumber(1042L),
            new UserId(UuidV7.randomUuid()),
            UuidV7.randomUuid(),
            "Quiz 1",
            TimelinePoint.at(now),
            List.of(sampleQuizzSlide(1, 20, 1000)));

    assertThat(session.hasEnoughParticipantsToStart()).isTrue();
    session.start(TimelinePoint.at(now));

    assertThat(session.getStatus()).isEqualTo("STARTED");
    assertThat(session.getCurrentSlideIndex()).isEqualTo(1);
    assertThat(session.getCurrentSlideStartedAt()).isEqualTo(TimelinePoint.at(now));
    assertThat(session.isPaused()).isFalse();

    final var endResult = session.advanceToNextSlide(TimelinePoint.at(now.plusSeconds(25)));
    assertThat(endResult.ended()).isTrue();
    assertThat(session.getStatus()).isEqualTo("ENDED");
    assertThat(session.getEndedAt()).isEqualTo(TimelinePoint.at(now.plusSeconds(25)));
  }

  @Test
  void shouldPersistPausedAtAndShiftSlideStartOnResume() {
    final var t0 = Instant.parse("2026-11-18T10:00:00Z");
    final var session =
        Session.createLive(
            new SessionId(UuidV7.randomUuid()),
            "Pause Test",
            new SessionNumber(2042L),
            new UserId(UuidV7.randomUuid()),
            UuidV7.randomUuid(),
            "Quiz Pause",
            TimelinePoint.at(t0),
            List.of(sampleQuizzSlide(1, 20, 1000)));

    session.start(TimelinePoint.at(t0));
    session.setPaused(true, TimelinePoint.at(t0.plusSeconds(5)));

    assertThat(session.isPaused()).isTrue();
    assertThat(session.getPausedAt()).isEqualTo(TimelinePoint.at(t0.plusSeconds(5)));

    // Submitting while paused must fail with CONFLICT (SC-BUG-02)
    assertThatThrownBy(() -> session.evaluateAnswer(1, 0, null, t0.plusSeconds(10)))
        .isInstanceOf(SessionDomainException.class)
        .satisfies(
            ex ->
                assertThat(((SessionDomainException) ex).category())
                    .isEqualTo(SessionDomainException.ErrorCategory.CONFLICT));

    // Resume 15 seconds later -> currentSlideStartedAt shifts by +15s
    session.setPaused(false, TimelinePoint.at(t0.plusSeconds(20)));
    assertThat(session.isPaused()).isFalse();
    assertThat(session.getPausedAt()).isNull();
    assertThat(session.getCurrentSlideStartedAt()).isEqualTo(TimelinePoint.at(t0.plusSeconds(15)));

    // Now at t0+25s, only 10s have elapsed since shifted start -> valid answer!
    final var eval = session.evaluateAnswer(1, 0, null, t0.plusSeconds(25));
    assertThat(eval.isCorrect()).isTrue();
    assertThat(eval.pointsAwarded()).isEqualTo(750);
  }

  @Test
  void shouldBreakScoreTiesDeterministicallyByResponseTimeThenArrivalOrder() {
    final var t0 = Instant.parse("2026-11-18T10:00:00Z");
    final var slow =
        new SessionParticipant(UUID.randomUUID(), "Slow", 1000, "tok1", false, t0, 8000L, 1, 8000L);
    final var fastLateJoin =
        new SessionParticipant(
            UUID.randomUUID(), "FastLate", 1000, "tok2", false, t0.plusSeconds(5), 2000L, 1, 2000L);
    final var fastEarlyJoin =
        new SessionParticipant(
            UUID.randomUUID(),
            "FastEarly",
            1000,
            "tok3",
            false,
            t0.plusSeconds(1),
            2000L,
            1,
            2000L);

    final var session =
        Session.hydrateLive(
            new SessionId(UuidV7.randomUuid()),
            "Tie Break",
            new SessionNumber(3000L),
            2,
            new UserId(UuidV7.randomUuid()),
            UuidV7.randomUuid(),
            "Quiz",
            ProgressionMode.MANUAL,
            false,
            15,
            false,
            TimelinePoint.at(t0),
            TimelinePoint.at(t0),
            null,
            null,
            2,
            TimelinePoint.at(t0),
            List.of(sampleQuizzSlide(1, 20, 1000), sampleQuizzSlide(2, 20, 1000)),
            List.of(slow, fastLateJoin, fastEarlyJoin),
            List.of(
                new SessionAnswerRecord(
                    UUID.randomUUID(), "FastEarly", 1, 0, null, 500, true, t0.plusSeconds(1)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "FastEarly", 2, 0, null, 500, true, t0.plusSeconds(2)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "FastLate", 1, 1, null, 0, false, t0.plusSeconds(1)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "FastLate", 2, 0, null, 1000, true, t0.plusSeconds(2))));

    final var leaderboard = session.buildLeaderboardEntries(2);
    assertThat(leaderboard)
        .extracting(SessionLeaderboardEntry::participantName)
        .containsExactly("FastEarly", "FastLate", "Slow");

    // Verify real delta and streak (SC-BUG-06)
    assertThat(leaderboard.get(0).delta()).isEqualTo(500);
    assertThat(leaderboard.get(0).streak()).isEqualTo(2);
    assertThat(leaderboard.get(1).delta()).isEqualTo(1000);
    assertThat(leaderboard.get(1).streak()).isEqualTo(1);
    assertThat(leaderboard.get(2).delta()).isZero();
    assertThat(leaderboard.get(2).streak()).isZero();
  }
}

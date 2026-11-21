package com.quezap.api.v1.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.usecases.sessions.SessionReportApplicationService;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class SessionReportServiceTest {

  private SessionReportService sessionReportService;

  @BeforeEach
  void setUp() {
    final var store = Mockito.mock(SessionStorePort.class);
    sessionReportService = new SessionReportService(new SessionReportApplicationService(store));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "=cmd|' /C calc'!A0",
        "+cmd|' /C calc'!A0",
        "-cmd|' /C calc'!A0",
        "@SUM(A1:A10)",
        "\t=cmd",
        "\r=calc",
        "\n=CMD()",
        "|malicious",
        " =cmd|calc",
        "   +calc",
        "  @eval"
      })
  void shouldEscapePotentiallyDangerousFormulaPrefixes(String formula) {
    final var escaped = SessionReportService.escapeCsvCell(formula);
    assertThat(escaped).startsWith("\"'");
    assertThat(escaped).endsWith("\"");
  }

  @Test
  void shouldNeutralizeFormulasAfterLineBreaks() {
    assertThat(SessionReportService.escapeCsvCell("safe\n=CMD()")).isEqualTo("\"safe\n'=CMD()\"");
    assertThat(SessionReportService.escapeCsvCell("safe\r+1+1")).isEqualTo("\"safe\r'+1+1\"");
    assertThat(SessionReportService.escapeCsvCell("\n=CMD()")).isEqualTo("\"'\n'=CMD()\"");
  }

  @Test
  void shouldHandleNullAndSafeValuesInCsvEscape() {
    assertThat(SessionReportService.escapeCsvCell(null)).isEqualTo("\"\"");
    assertThat(SessionReportService.escapeCsvCell("Normal User")).isEqualTo("\"Normal User\"");
    assertThat(SessionReportService.escapeCsvCell("User \"Quoted\""))
        .isEqualTo("\"User \"\"Quoted\"\"\"");
  }

  @Test
  void shouldGenerateCsvExportWithUtf8BomCharsetAndNeutralizedCells() {
    final var now = Instant.now();
    final var session =
        Session.hydrateLive(
            new SessionId(UuidV7.randomUuid()),
            "Session CSV Test",
            new SessionNumber(99999L),
            1,
            new UserId(UuidV7.randomUuid()),
            null,
            null,
            ProgressionMode.MANUAL,
            false,
            15,
            false,
            null,
            null,
            null,
            null,
            0,
            TimelinePoint.at(now),
            List.of(),
            List.of(
                new SessionParticipant(
                    UUID.randomUUID(), " =calc", 250, "token_test", false, now, 1000L, 1, 1000L)),
            List.of());

    final var response = sessionReportService.exportCsv(session);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(new MediaType("text", "csv", StandardCharsets.UTF_8));
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).startsWith("\uFEFFRang,Pseudo");
    assertThat(response.getBody()).contains("\"' =calc\"");
  }

  @Test
  void shouldComputeRealDeltaStreakAndDeterministicTieBreakInBuildScores() {
    final var t0 = Instant.parse("2026-11-18T10:00:00Z");
    final var session =
        Session.hydrateLive(
            new SessionId(UuidV7.randomUuid()),
            "Scores Test",
            new SessionNumber(4242L),
            2,
            new UserId(UuidV7.randomUuid()),
            null,
            null,
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
            List.of(),
            List.of(
                new SessionParticipant(
                    UUID.randomUUID(), "Bob", 1000, "t1", false, t0, 5000L, 2, 2000L),
                new SessionParticipant(
                    UUID.randomUUID(),
                    "Alice",
                    1000,
                    "t2",
                    false,
                    t0.plusSeconds(3),
                    2500L,
                    2,
                    1000L)),
            List.of(
                new SessionAnswerRecord(
                    UUID.randomUUID(), "Alice", 1, 0, null, 600, true, t0.plusSeconds(1)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "Alice", 2, 0, null, 400, true, t0.plusSeconds(2)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "Bob", 1, 0, null, 1000, true, t0.plusSeconds(1)),
                new SessionAnswerRecord(
                    UUID.randomUUID(), "Bob", 2, 1, null, 0, false, t0.plusSeconds(2))));

    final var scores = sessionReportService.buildScores(session);

    assertThat(scores.leaderboard()).hasSize(2);
    assertThat(scores.leaderboard().get(0).participantName()).isEqualTo("Alice");
    assertThat(scores.leaderboard().get(0).delta()).isEqualTo(400);
    assertThat(scores.leaderboard().get(0).streak()).isEqualTo(2);

    assertThat(scores.leaderboard().get(1).participantName()).isEqualTo("Bob");
    assertThat(scores.leaderboard().get(1).delta()).isZero();
    assertThat(scores.leaderboard().get(1).streak()).isZero();
  }

  @Test
  void shouldMaskCurrentSlideScoreUntilRevealedAndShowAfterEnd() {
    final var t0 = Instant.parse("2026-11-18T10:00:00Z");
    final var session =
        Session.hydrateLive(
            new SessionId(UuidV7.randomUuid()),
            "Scores Oracle Test",
            new SessionNumber(4243L),
            1,
            new UserId(UuidV7.randomUuid()),
            null,
            null,
            ProgressionMode.MANUAL,
            false,
            15,
            false,
            TimelinePoint.at(t0),
            TimelinePoint.at(t0),
            null,
            null,
            null,
            TimelinePoint.at(t0),
            List.of(),
            List.of(
                new SessionParticipant(
                    UUID.randomUUID(), "Alice", 950, "t1", false, t0, 1000L, 1, 1000L)),
            List.of(
                new SessionAnswerRecord(
                    UUID.randomUUID(), "Alice", 1, 0, null, 950, true, t0.plusSeconds(1))));

    final var beforeReveal = sessionReportService.buildScores(session);
    assertThat(beforeReveal.leaderboard()).hasSize(1);
    assertThat(beforeReveal.leaderboard().get(0).score()).isZero();
    assertThat(beforeReveal.leaderboard().get(0).delta()).isZero();
    assertThat(beforeReveal.leaderboard().get(0).streak()).isZero();

    final var endedSession =
        Session.hydrateLive(
            session.getId(),
            "Scores Oracle Test",
            new SessionNumber(4243L),
            1,
            session.getAuthor(),
            null,
            null,
            ProgressionMode.MANUAL,
            false,
            15,
            false,
            TimelinePoint.at(t0),
            TimelinePoint.at(t0),
            null,
            TimelinePoint.at(t0.plusSeconds(30)),
            null,
            TimelinePoint.at(t0),
            List.of(),
            session.getSessionParticipants(),
            session.getSessionAnswers());

    final var afterEnd = sessionReportService.buildScores(endedSession);
    assertThat(afterEnd.leaderboard().get(0).score()).isEqualTo(950);
    assertThat(afterEnd.leaderboard().get(0).delta()).isEqualTo(950);
    assertThat(afterEnd.leaderboard().get(0).streak()).isEqualTo(1);
  }
}

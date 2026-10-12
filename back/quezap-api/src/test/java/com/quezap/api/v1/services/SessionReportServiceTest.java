package com.quezap.api.v1.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionReportServiceTest {

  private SessionReportService sessionReportService;

  @BeforeEach
  void setUp() {
    sessionReportService = new SessionReportService();
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
  void shouldHandleNullAndSafeValuesInCsvEscape() {
    assertThat(SessionReportService.escapeCsvCell(null)).isEqualTo("\"\"");
    assertThat(SessionReportService.escapeCsvCell("Normal User")).isEqualTo("\"Normal User\"");
    assertThat(SessionReportService.escapeCsvCell("User \"Quoted\""))
        .isEqualTo("\"User \"\"Quoted\"\"\"");
  }

  @Test
  void shouldGenerateCsvExportWithNeutralizedCells() {
    final var sessionId = UUID.randomUUID();
    final var sessionEntity =
        new SessionJpaEntity(
            sessionId,
            99999L,
            "Session CSV Test",
            1,
            UUID.randomUUID(),
            null,
            null,
            null,
            Instant.now());
    final var participant =
        new SessionParticipantJpaEntity(
            UUID.randomUUID(), sessionEntity, " =calc", 250, "token_test", false, Instant.now());
    sessionEntity.addParticipant(participant);

    final var response = sessionReportService.exportCsv(sessionEntity);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains("\"' =calc\"");
  }
}

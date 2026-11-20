package com.quezap.api.v1.services;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.QuestionAnswerStatDto;
import com.quezap.api.v1.dto.response.sessions.QuestionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionParticipantReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.application.usecases.sessions.SessionReportApplicationService;
import com.quezap.application.usecases.sessions.SessionReportApplicationService.CsvExportData;
import com.quezap.application.usecases.sessions.SessionReportApplicationService.ScoresView;
import com.quezap.application.usecases.sessions.SessionReportApplicationService.SessionReportView;
import com.quezap.domain.models.entities.Session;

import org.jspecify.annotations.Nullable;

@Service
public class SessionReportService {

  private static final MediaType TEXT_CSV_UTF8 =
      new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final SessionReportApplicationService reportApplicationService;

  public SessionReportService(SessionReportApplicationService reportApplicationService) {
    this.reportApplicationService = reportApplicationService;
  }

  public static String escapeCsvCell(@Nullable String value) {
    return SessionReportApplicationService.escapeCsvCell(value);
  }

  public ResponseEntity<String> exportCsv(UUID sessionId, UUID requesterId) {
    final var csvData = reportApplicationService.buildCsvExport(sessionId, requesterId);
    return toCsvResponse(csvData);
  }

  public ResponseEntity<String> exportCsv(Session session) {
    final var csvData = reportApplicationService.generateCsvFromSession(session);
    return toCsvResponse(csvData);
  }

  public LeaderboardResponseDto buildScores(UUID sessionId) {
    return toLeaderboardDto(reportApplicationService.buildScores(sessionId));
  }

  public LeaderboardResponseDto buildScores(Session session) {
    return toLeaderboardDto(reportApplicationService.buildScoresFromSession(session));
  }

  public SessionReportDto buildSessionReport(UUID sessionId, UUID requesterId) {
    return toSessionReportDto(reportApplicationService.buildSessionReport(sessionId, requesterId));
  }

  public SessionReportDto buildSessionReport(Session session) {
    return toSessionReportDto(reportApplicationService.buildReportFromSession(session));
  }

  private static ResponseEntity<String> toCsvResponse(CsvExportData csvData) {
    return ResponseEntity.ok()
        .contentType(TEXT_CSV_UTF8)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csvData.filename() + "\"")
        .body(csvData.content());
  }

  private static LeaderboardResponseDto toLeaderboardDto(ScoresView view) {
    final var entries =
        view.leaderboard().stream()
            .map(
                e ->
                    new LeaderboardEntryDto(
                        e.rank(), e.participantName(), e.score(), e.delta(), e.streak()))
            .toList();
    return new LeaderboardResponseDto(view.sessionId(), view.currentSlideIndex(), entries);
  }

  private static SessionReportDto toSessionReportDto(SessionReportView view) {
    final var participants =
        view.participants().stream()
            .map(
                p ->
                    new SessionParticipantReportDto(
                        p.rank(),
                        p.participantName(),
                        p.totalScore(),
                        p.correctCount(),
                        p.totalQuestions(),
                        p.accuracyRate()))
            .toList();

    final var questions =
        view.questions().stream()
            .map(
                q ->
                    new QuestionReportDto(
                        q.slideIndex(),
                        q.questionLabel(),
                        q.questionType(),
                        q.totalAnswers(),
                        q.correctAnswers(),
                        q.successRate(),
                        q.answers().stream()
                            .map(
                                a ->
                                    new QuestionAnswerStatDto(
                                        a.answerIndex(),
                                        a.label(),
                                        a.isCorrect(),
                                        a.count(),
                                        a.percentage()))
                            .toList()))
            .toList();

    return new SessionReportDto(
        view.sessionId(),
        view.sessionNumber(),
        view.sessionCode(),
        view.sessionName(),
        view.startedAt(),
        view.endedAt(),
        view.totalParticipants(),
        view.totalQuestions(),
        view.averageScore(),
        view.averageAccuracyRate(),
        participants,
        questions);
  }
}

package com.quezap.application.usecases.sessions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;
import com.quezap.domain.services.TextNormalizer;

import org.jspecify.annotations.Nullable;

@Service
public class SessionReportApplicationService {

  public static final String UTF8_BOM = "\uFEFF";

  public record CsvExportData(String filename, String content) {}

  public record ScoresView(
      UUID sessionId, int currentSlideIndex, List<SessionLeaderboardEntry> leaderboard) {}

  public record ParticipantReportView(
      int rank,
      String participantName,
      int totalScore,
      int correctCount,
      int totalQuestions,
      double accuracyRate) {}

  public record AnswerStatView(
      int answerIndex, String label, boolean isCorrect, int count, double percentage) {}

  public record QuestionReportView(
      int slideIndex,
      String questionLabel,
      String questionType,
      int totalAnswers,
      int correctAnswers,
      double successRate,
      List<AnswerStatView> answers) {}

  public record SessionReportView(
      UUID sessionId,
      long sessionNumber,
      String sessionCode,
      String sessionName,
      @Nullable Instant startedAt,
      @Nullable Instant endedAt,
      int totalParticipants,
      int totalQuestions,
      double averageScore,
      double averageAccuracyRate,
      List<ParticipantReportView> participants,
      List<QuestionReportView> questions) {}

  private final SessionStorePort sessionStore;

  public SessionReportApplicationService(SessionStorePort sessionStore) {
    this.sessionStore = sessionStore;
  }

  public static String escapeCsvCell(@Nullable String value) {
    if (value == null) {
      return "\"\"";
    }
    final var sb = new StringBuilder(value.length() + 8);
    var lineStart = true;
    var lineStartIndex = 0;

    for (var i = 0; i < value.length(); i++) {
      final var ch = value.charAt(i);
      if (lineStart) {
        if (ch == '\r' || ch == '\n') {
          sb.append(ch);
          lineStartIndex = sb.length();
          continue;
        }
        if (ch == ' ' || ch == '\t') {
          if (ch == '\t' && sb.length() == lineStartIndex) {
            sb.insert(lineStartIndex, '\'');
            lineStart = false;
          }
          sb.append(ch);
          continue;
        }
        if (isFormulaTrigger(ch)) {
          sb.insert(lineStartIndex, '\'');
        }
        lineStart = false;
        sb.append(ch);
      } else {
        sb.append(ch);
        if (ch == '\r' || ch == '\n') {
          lineStart = true;
          lineStartIndex = sb.length();
        }
      }
    }

    var result = sb.toString();
    final var trimmedLeading = value.stripLeading();
    if (!trimmedLeading.isEmpty()
        && isFormulaTrigger(trimmedLeading.charAt(0))
        && !result.startsWith("'")) {
      result = "'" + result;
    }
    return "\"" + result.replace("\"", "\"\"") + "\"";
  }

  private static boolean isFormulaTrigger(char ch) {
    return ch == '='
        || ch == '+'
        || ch == '-'
        || ch == '@'
        || ch == '\t'
        || ch == '\r'
        || ch == '|';
  }

  @Transactional(readOnly = true)
  public CsvExportData buildCsvExport(UUID sessionId, UUID requesterId) {
    final var session =
        sessionStore
            .findSessionById(sessionId)
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));
    session.assertHost(requesterId);
    return generateCsvFromSession(session);
  }

  public CsvExportData generateCsvFromSession(Session session) {
    final var participants = session.getRankedActiveParticipants();
    final var totalQuestions = session.getTotalSlides();
    final var answersByParticipant =
        Session.indexAnswersByParticipantAndSlide(session.getSessionAnswers());

    final var sb = new StringBuilder(UTF8_BOM);
    sb.append("Rang,Pseudo,Score Total,Bonnes Réponses,Total Questions,Taux de Réussite (%)\n");

    var rank = 1;
    for (final var p : participants) {
      final var slideAnswers = answersByParticipant.getOrDefault(p.name(), Map.of());
      var correctCount = 0;
      for (final var a : slideAnswers.values()) {
        if (a.isCorrect()) {
          correctCount++;
        }
      }
      final var accuracy =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      sb.append(
          String.format(
              Locale.US,
              "%d,%s,%d,%d,%d,%.1f%%\n",
              rank++,
              escapeCsvCell(p.name()),
              p.score(),
              correctCount,
              totalQuestions,
              accuracy));
    }

    final var filename = "session-" + session.getNumber().value() + "-resultats.csv";
    return new CsvExportData(filename, sb.toString());
  }

  @Transactional(readOnly = true)
  public ScoresView buildScores(UUID sessionId) {
    final var session =
        sessionStore
            .findSessionById(sessionId)
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));
    return buildScoresFromSession(session);
  }

  public ScoresView buildScoresFromSession(Session session) {
    final var slideIndex = session.getCurrentSlideIndex();
    final var entries = session.buildPublicLeaderboardEntries();
    return new ScoresView(session.getId().value(), slideIndex, entries);
  }

  @Transactional(readOnly = true)
  public SessionReportView buildSessionReport(UUID sessionId, UUID requesterId) {
    final var session =
        sessionStore
            .findSessionById(sessionId)
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));
    session.assertHost(requesterId);
    return buildReportFromSession(session);
  }

  public SessionReportView buildReportFromSession(Session session) {
    final var slides = session.getSlideSpecs();
    final var totalQuestions = slides.size();
    final var activeParticipants = session.getRankedActiveParticipants();
    final var totalParticipants = activeParticipants.size();

    final var allAnswers = session.getSessionAnswers();
    final var answersByParticipant = Session.indexAnswersByParticipantAndSlide(allAnswers);
    final var kickedNames = new HashSet<String>();
    for (final var p : session.getSessionParticipants()) {
      if (p.isKicked()) {
        kickedNames.add(p.name());
      }
    }
    final var activeAnswersBySlide = Session.indexActiveAnswersBySlide(allAnswers, kickedNames);

    var rank = 1;
    final var participantReports = new ArrayList<ParticipantReportView>(totalParticipants);
    var totalScoreSum = 0L;
    var totalAccuracySum = 0.0;

    for (final var p : activeParticipants) {
      final var slideMap = answersByParticipant.getOrDefault(p.name(), Map.of());
      var correctCount = 0;
      for (final var a : slideMap.values()) {
        if (a.isCorrect()) {
          correctCount++;
        }
      }
      final var accuracyRate =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      totalScoreSum += p.score();
      totalAccuracySum += accuracyRate;

      participantReports.add(
          new ParticipantReportView(
              rank++,
              p.name(),
              p.score(),
              correctCount,
              totalQuestions,
              Math.round(accuracyRate * 10.0) / 10.0));
    }

    final var averageScore =
        totalParticipants > 0 ? (double) totalScoreSum / totalParticipants : 0.0;
    final var averageAccuracyRate =
        totalParticipants > 0 ? totalAccuracySum / totalParticipants : 0.0;

    final var questionReports = new ArrayList<QuestionReportView>(slides.size());
    for (var idx = 0; idx < slides.size(); idx++) {
      final var slide = slides.get(idx);
      final var slideIndex = idx + 1;
      final var slideAnswers = activeAnswersBySlide.getOrDefault(slideIndex, List.of());

      final var slideTotalAnswers = slideAnswers.size();
      final var slideCorrectAnswers =
          (int) slideAnswers.stream().filter(SessionAnswerRecord::isCorrect).count();
      final var successRate =
          slideTotalAnswers > 0 ? (double) slideCorrectAnswers / slideTotalAnswers * 100.0 : 0.0;

      final var answerStats = buildAnswerStats(slide, slideAnswers, slideTotalAnswers);
      questionReports.add(
          new QuestionReportView(
              slideIndex,
              slide.label() != null ? slide.label() : "",
              slide.normalizedType(),
              slideTotalAnswers,
              slideCorrectAnswers,
              Math.round(successRate * 10.0) / 10.0,
              answerStats));
    }

    return new SessionReportView(
        session.getId().value(),
        session.getNumber().value(),
        String.valueOf(session.getNumber().value()),
        session.getRawName(),
        session.getStartedAt() != null ? session.getStartedAt().position() : null,
        session.getEndedAt() != null ? session.getEndedAt().position() : null,
        totalParticipants,
        totalQuestions,
        Math.round(averageScore * 10.0) / 10.0,
        Math.round(averageAccuracyRate * 10.0) / 10.0,
        participantReports,
        questionReports);
  }

  private static List<AnswerStatView> buildAnswerStats(
      com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec slide,
      List<SessionAnswerRecord> slideAnswers,
      int slideTotalAnswers) {
    final var stats = new ArrayList<AnswerStatView>();
    final var options = slide.options();
    if (slide.isBooleanQuestion()) {
      final var statementIsTrue = !options.isEmpty() && options.get(0).isCorrect();
      for (var answerIndex = 0; answerIndex < 2; answerIndex++) {
        final var target = answerIndex;
        final var count =
            (int) slideAnswers.stream().filter(sa -> sa.answerIndex() == target).count();
        final var percentage =
            slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
        final var label = answerIndex == 0 ? "Vrai" : "Faux";
        final var isOptCorrect = answerIndex == 0 ? statementIsTrue : !statementIsTrue;
        stats.add(
            new AnswerStatView(
                answerIndex, label, isOptCorrect, count, Math.round(percentage * 10.0) / 10.0));
      }
    } else if (slide.isCashQuestion()) {
      for (var answerIndex = 0; answerIndex < options.size(); answerIndex++) {
        final var opt = options.get(answerIndex);
        final var expectedLabel = opt.label() != null ? opt.label() : "";
        final var count =
            (int)
                slideAnswers.stream()
                    .filter(
                        sa ->
                            sa.textAnswer() != null
                                && TextNormalizer.matches(sa.textAnswer(), expectedLabel))
                    .count();
        final var percentage =
            slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
        stats.add(
            new AnswerStatView(
                answerIndex,
                expectedLabel,
                opt.isCorrect(),
                count,
                Math.round(percentage * 10.0) / 10.0));
      }
    } else {
      for (var answerIndex = 0; answerIndex < options.size(); answerIndex++) {
        final var opt = options.get(answerIndex);
        final var target = answerIndex;
        final var count =
            (int) slideAnswers.stream().filter(sa -> sa.answerIndex() == target).count();
        final var percentage =
            slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
        stats.add(
            new AnswerStatView(
                answerIndex,
                opt.label() != null ? opt.label() : "",
                opt.isCorrect(),
                count,
                Math.round(percentage * 10.0) / 10.0));
      }
    }
    return stats;
  }
}

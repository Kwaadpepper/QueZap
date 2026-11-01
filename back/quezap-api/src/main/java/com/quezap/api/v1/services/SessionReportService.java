package com.quezap.api.v1.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.dto.response.sessions.LeaderboardResponseDto;
import com.quezap.api.v1.dto.response.sessions.QuestionAnswerStatDto;
import com.quezap.api.v1.dto.response.sessions.QuestionReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionParticipantReportDto;
import com.quezap.api.v1.dto.response.sessions.SessionReportDto;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;

import org.jspecify.annotations.Nullable;

@Service
public class SessionReportService {

  public static String escapeCsvCell(@Nullable String value) {
    if (value == null) {
      return "\"\"";
    }
    var sanitized = value;
    final var trimmedLeading = sanitized.stripLeading();
    if (!trimmedLeading.isEmpty()) {
      final var firstChar = trimmedLeading.charAt(0);
      if (firstChar == '='
          || firstChar == '+'
          || firstChar == '-'
          || firstChar == '@'
          || firstChar == '\t'
          || firstChar == '\r'
          || firstChar == '|') {
        sanitized = "'" + sanitized;
      }
    }
    return "\"" + sanitized.replace("\"", "\"\"") + "\"";
  }

  public ResponseEntity<String> exportCsv(SessionJpaEntity sessionJpa) {
    final var participants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    final var totalQuestions =
        sessionJpa.getQuezap() != null ? sessionJpa.getQuezap().getQuestions().size() : 0;

    final var sb = new StringBuilder();
    sb.append("Rang,Pseudo,Score Total,Bonnes Réponses,Total Questions,Taux de Réussite (%)\n");

    var rank = 1;
    for (final var p : participants) {
      final var correctCount =
          (int)
              sessionJpa.getAnswers().stream()
                  .filter(
                      a ->
                          a.getParticipantName().equals(p.getName())
                              && Boolean.TRUE.equals(a.getIsCorrect()))
                  .count();
      final var accuracy =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      sb.append(
          String.format(
              Locale.US,
              "%d,%s,%d,%d,%d,%.1f%%\n",
              rank++,
              escapeCsvCell(p.getName()),
              p.getScore(),
              correctCount,
              totalQuestions,
              accuracy));
    }

    final var filename = "session-" + sessionJpa.getNumber() + "-resultats.csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(sb.toString());
  }

  public LeaderboardResponseDto buildScores(SessionJpaEntity sessionJpa) {
    final var participants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    var rank = 1;
    final var leaderboard = new ArrayList<LeaderboardEntryDto>();
    for (final var p : participants) {
      leaderboard.add(new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1));
    }

    return new LeaderboardResponseDto(
        sessionJpa.getId(), sessionJpa.getCurrentSlideIndex(), leaderboard);
  }

  public SessionReportDto buildSessionReport(SessionJpaEntity sessionJpa) {
    final var quezap = sessionJpa.getQuezap();
    final var questionsList =
        quezap != null
            ? quezap.getQuestions().stream()
                .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
                .toList()
            : List.<QuezapQuestionJpaEntity>of();

    final var totalQuestions = questionsList.size();

    final var activeParticipants =
        sessionJpa.getParticipants().stream()
            .filter(p -> !p.getIsKicked())
            .sorted(Comparator.comparing(SessionParticipantJpaEntity::getScore).reversed())
            .toList();

    final var totalParticipants = activeParticipants.size();

    var rank = 1;
    final var participantReports = new ArrayList<SessionParticipantReportDto>();
    var totalScoreSum = 0L;
    var totalAccuracySum = 0.0;

    for (final var p : activeParticipants) {
      final var correctCount =
          (int)
              sessionJpa.getAnswers().stream()
                  .filter(
                      a ->
                          a.getParticipantName().equals(p.getName())
                              && Boolean.TRUE.equals(a.getIsCorrect()))
                  .count();
      final var accuracyRate =
          totalQuestions > 0 ? (double) correctCount / totalQuestions * 100.0 : 0.0;
      totalScoreSum += p.getScore();
      totalAccuracySum += accuracyRate;

      participantReports.add(
          new SessionParticipantReportDto(
              rank++,
              p.getName(),
              p.getScore(),
              correctCount,
              totalQuestions,
              Math.round(accuracyRate * 10.0) / 10.0));
    }

    final var averageScore =
        totalParticipants > 0 ? (double) totalScoreSum / totalParticipants : 0.0;
    final var averageAccuracyRate =
        totalParticipants > 0 ? totalAccuracySum / totalParticipants : 0.0;

    final var questionReports = new ArrayList<QuestionReportDto>();
    for (var idx = 0; idx < questionsList.size(); idx++) {
      final var qq = questionsList.get(idx);
      final var slideIndex = idx + 1;
      final var q = qq.getQuestion();
      final var answersList =
          q.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .toList();

      final var slideAnswers =
          sessionJpa.getAnswers().stream()
              .filter(a -> a.getSlideIndex().equals(slideIndex))
              .toList();

      final var slideTotalAnswers = slideAnswers.size();
      final var slideCorrectAnswers =
          (int) slideAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
      final var successRate =
          slideTotalAnswers > 0 ? (double) slideCorrectAnswers / slideTotalAnswers * 100.0 : 0.0;

      final var isBoolean = "BOOLEAN".equalsIgnoreCase(q.getType());
      final var isCash = "CASH".equalsIgnoreCase(q.getType());
      final var answerStats = new ArrayList<QuestionAnswerStatDto>();
      if (isBoolean) {
        final var answerEntity = answersList.isEmpty() ? null : answersList.get(0);
        final var statementIsTrue =
            answerEntity != null && Boolean.TRUE.equals(answerEntity.getIsCorrect());
        for (var answerIndex = 0; answerIndex < 2; answerIndex++) {
          final var targetIndex = answerIndex;
          final var count =
              (int)
                  slideAnswers.stream()
                      .filter(sa -> sa.getAnswerIndex().equals(targetIndex))
                      .count();
          final var percentage =
              slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
          final var label = answerIndex == 0 ? "Vrai" : "Faux";
          final var isOptCorrect = answerIndex == 0 ? statementIsTrue : !statementIsTrue;
          answerStats.add(
              new QuestionAnswerStatDto(
                  answerIndex, label, isOptCorrect, count, Math.round(percentage * 10.0) / 10.0));
        }
      } else if (isCash) {
        for (var answerIndex = 0; answerIndex < answersList.size(); answerIndex++) {
          final var ans = answersList.get(answerIndex);
          final var expectedLabel = ans.getLabel() != null ? ans.getLabel() : "";
          final var count =
              (int)
                  slideAnswers.stream()
                      .filter(
                          sa ->
                              sa.getTextAnswer() != null
                                  && com.quezap.domain.services.TextNormalizer.matches(
                                      sa.getTextAnswer(), expectedLabel))
                      .count();
          final var percentage =
              slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
          answerStats.add(
              new QuestionAnswerStatDto(
                  answerIndex,
                  expectedLabel,
                  Boolean.TRUE.equals(ans.getIsCorrect()),
                  count,
                  Math.round(percentage * 10.0) / 10.0));
        }
      } else {
        for (var answerIndex = 0; answerIndex < answersList.size(); answerIndex++) {
          final var ans = answersList.get(answerIndex);
          final var targetIndex = answerIndex;
          final var count =
              (int)
                  slideAnswers.stream()
                      .filter(sa -> sa.getAnswerIndex().equals(targetIndex))
                      .count();
          final var percentage =
              slideTotalAnswers > 0 ? (double) count / slideTotalAnswers * 100.0 : 0.0;
          answerStats.add(
              new QuestionAnswerStatDto(
                  answerIndex,
                  ans.getLabel() != null ? ans.getLabel() : "",
                  Boolean.TRUE.equals(ans.getIsCorrect()),
                  count,
                  Math.round(percentage * 10.0) / 10.0));
        }
      }

      questionReports.add(
          new QuestionReportDto(
              slideIndex,
              q.getLabel() != null ? q.getLabel() : "",
              q.getType() != null ? q.getType() : "QUIZZ",
              slideTotalAnswers,
              slideCorrectAnswers,
              Math.round(successRate * 10.0) / 10.0,
              answerStats));
    }

    return new SessionReportDto(
        sessionJpa.getId(),
        sessionJpa.getNumber(),
        String.valueOf(sessionJpa.getNumber()),
        sessionJpa.getName(),
        sessionJpa.getStartedAt(),
        sessionJpa.getEndedAt(),
        totalParticipants,
        totalQuestions,
        Math.round(averageScore * 10.0) / 10.0,
        Math.round(averageAccuracyRate * 10.0) / 10.0,
        participantReports,
        questionReports);
  }
}

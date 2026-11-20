package com.quezap.application.ports.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;

import org.jspecify.annotations.Nullable;

public interface SessionRealtimePublisher {

  record ParticipantView(String name, Instant joinedAt) {}

  record SafeOptionView(int index, @Nullable String label, @Nullable String pictureUrl) {}

  record SafeQuestionView(
      UUID id,
      String type,
      String label,
      @Nullable String pictureUrl,
      List<SafeOptionView> answers) {}

  record QuestionStartEvent(
      int slideIndex,
      int totalSlides,
      int timerSeconds,
      int points,
      SafeQuestionView question,
      Instant deadline,
      int answeredCount,
      int remainingSeconds) {}

  record OptionStatView(int answerIndex, int count, double percentage) {}

  record QuestionRevealEvent(
      int slideIndex,
      List<Integer> correctAnswerIndices,
      @Nullable String explanation,
      List<OptionStatView> answersStats,
      boolean mobileCorrectionEnabled,
      @Nullable List<String> acceptedAnswers) {}

  record GroupMetricsView(int totalParticipants, int averageScore, double globalSuccessRate) {}

  record SessionEndedEvent(
      Instant endedAt,
      List<SessionLeaderboardEntry> finalLeaderboard,
      String firstName,
      @Nullable String secondName,
      @Nullable String thirdName,
      GroupMetricsView metrics,
      List<SessionLeaderboardEntry> top5) {}

  void publishLobbyState(
      UUID sessionId,
      long sessionNumber,
      String sessionName,
      int totalQuestions,
      List<ParticipantView> participants);

  void publishSessionStarted(
      UUID sessionId, long sessionNumber, Instant startedAt, int totalSlides, int countdownSeconds);

  void publishQuestionStart(UUID sessionId, long sessionNumber, QuestionStartEvent event);

  void publishAnswerCount(
      UUID sessionId, long sessionNumber, int slideIndex, int totalAnswered, int totalActive);

  void publishQuestionReveal(UUID sessionId, long sessionNumber, QuestionRevealEvent event);

  void publishIntermediateLeaderboard(
      UUID sessionId, long sessionNumber, int slideIndex, List<SessionLeaderboardEntry> entries);

  void publishPauseStatus(UUID sessionId, long sessionNumber, boolean isPaused, Instant timestamp);

  void publishParticipantKicked(
      UUID sessionId, long sessionNumber, String participantName, String reason);

  void publishSessionEnded(UUID sessionId, long sessionNumber, SessionEndedEvent event);
}

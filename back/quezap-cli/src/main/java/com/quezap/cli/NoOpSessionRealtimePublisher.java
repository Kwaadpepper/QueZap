package com.quezap.cli;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;

@Component
public class NoOpSessionRealtimePublisher implements SessionRealtimePublisher {

  @Override
  public void publishLobbyState(
      UUID sessionId,
      long sessionNumber,
      String sessionName,
      int totalQuestions,
      List<ParticipantView> participants) {
    // CLI does not serve real-time SSE connections
  }

  @Override
  public void publishSessionStarted(
      UUID sessionId,
      long sessionNumber,
      Instant startedAt,
      int totalSlides,
      int countdownSeconds) {}

  @Override
  public void publishQuestionStart(UUID sessionId, long sessionNumber, QuestionStartEvent event) {}

  @Override
  public void publishAnswerCount(
      UUID sessionId, long sessionNumber, int slideIndex, int totalAnswered, int totalActive) {}

  @Override
  public void publishQuestionReveal(
      UUID sessionId, long sessionNumber, QuestionRevealEvent event) {}

  @Override
  public void publishIntermediateLeaderboard(
      UUID sessionId, long sessionNumber, int slideIndex, List<SessionLeaderboardEntry> entries) {}

  @Override
  public void publishPauseStatus(
      UUID sessionId, long sessionNumber, boolean isPaused, Instant timestamp) {}

  @Override
  public void publishParticipantKicked(
      UUID sessionId, long sessionNumber, String participantName, String reason) {}

  @Override
  public void publishSessionEnded(UUID sessionId, long sessionNumber, SessionEndedEvent event) {}
}

package com.quezap.api.v1.routes.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.quezap.api.v1.dto.events.AnswerCountPayload;
import com.quezap.api.v1.dto.events.AnswerStatDto;
import com.quezap.api.v1.dto.events.FinalLeaderboardEntryDto;
import com.quezap.api.v1.dto.events.IntermediateLeaderboardPayload;
import com.quezap.api.v1.dto.events.LobbyStatePayload;
import com.quezap.api.v1.dto.events.ParticipantDto;
import com.quezap.api.v1.dto.events.ParticipantKickedPayload;
import com.quezap.api.v1.dto.events.PodiumDto;
import com.quezap.api.v1.dto.events.QuestionRevealPayload;
import com.quezap.api.v1.dto.events.QuestionStartPayload;
import com.quezap.api.v1.dto.events.SafeAnswerDto;
import com.quezap.api.v1.dto.events.SafeQuestionDto;
import com.quezap.api.v1.dto.events.SessionEndedPayload;
import com.quezap.api.v1.dto.events.SessionGroupMetricsDto;
import com.quezap.api.v1.dto.events.SessionPausePayload;
import com.quezap.api.v1.dto.events.SessionStartedPayload;
import com.quezap.api.v1.dto.response.sessions.LeaderboardEntryDto;
import com.quezap.api.v1.services.SessionSseService;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.usecases.sessions.SessionQueryService.InitialStreamData;
import com.quezap.domain.models.valueobjects.sessions.SessionLeaderboardEntry;

@Component
public class SessionSseRealtimePublisher implements SessionRealtimePublisher {

  private final SessionSseService sessionSseService;

  public SessionSseRealtimePublisher(SessionSseService sessionSseService) {
    this.sessionSseService = sessionSseService;
  }

  @Override
  public void publishLobbyState(
      UUID sessionId,
      long sessionNumber,
      String sessionName,
      int totalQuestions,
      List<ParticipantView> participants) {
    final var payload = toLobbyStatePayload(sessionName, totalQuestions, participants);
    sessionSseService.broadcast(sessionId, "LOBBY_STATE", sessionNumber, payload);
  }

  @Override
  public void publishSessionStarted(
      UUID sessionId,
      long sessionNumber,
      Instant startedAt,
      int totalSlides,
      int countdownSeconds) {
    final var payload = new SessionStartedPayload(startedAt, totalSlides, countdownSeconds);
    sessionSseService.broadcast(sessionId, "SESSION_STARTED", sessionNumber, payload);
  }

  @Override
  public void publishQuestionStart(UUID sessionId, long sessionNumber, QuestionStartEvent event) {
    final var payload = toQuestionStartPayload(event);
    sessionSseService.broadcast(sessionId, "QUESTION_START", sessionNumber, payload);
  }

  @Override
  public void publishAnswerCount(
      UUID sessionId, long sessionNumber, int slideIndex, int totalAnswered, int totalActive) {
    final var payload = new AnswerCountPayload(slideIndex, totalAnswered, totalActive);
    sessionSseService.broadcast(sessionId, "ANSWER_RECEIVED", sessionNumber, payload);
  }

  @Override
  public void publishQuestionReveal(UUID sessionId, long sessionNumber, QuestionRevealEvent event) {
    final var payload = toQuestionRevealPayload(event);
    sessionSseService.broadcast(sessionId, "QUESTION_REVEAL", sessionNumber, payload);
  }

  @Override
  public void publishIntermediateLeaderboard(
      UUID sessionId, long sessionNumber, int slideIndex, List<SessionLeaderboardEntry> entries) {
    final var payload = toIntermediateLeaderboardPayload(slideIndex, entries);
    sessionSseService.broadcast(sessionId, "INTERMEDIATE_LEADERBOARD", sessionNumber, payload);
  }

  @Override
  public void publishPauseStatus(
      UUID sessionId, long sessionNumber, boolean isPaused, Instant timestamp) {
    final var payload = new SessionPausePayload(isPaused, timestamp);
    sessionSseService.broadcast(sessionId, "SESSION_PAUSE_STATUS", sessionNumber, payload);
  }

  @Override
  public void publishParticipantKicked(
      UUID sessionId, long sessionNumber, String participantName, String reason) {
    final var payload = new ParticipantKickedPayload(participantName, reason);
    sessionSseService.broadcast(sessionId, "PARTICIPANT_KICKED", sessionNumber, payload);
  }

  @Override
  public void publishSessionEnded(UUID sessionId, long sessionNumber, SessionEndedEvent event) {
    final var payload = toSessionEndedPayload(event);
    sessionSseService.broadcast(sessionId, "SESSION_ENDED", sessionNumber, payload);
  }

  public void sendInitialState(SseEmitter emitter, UUID sessionId, InitialStreamData initialData) {
    final var sessionNumber = initialData.sessionNumber();
    final var lobbyPayload =
        toLobbyStatePayload(
            initialData.sessionName(), initialData.totalQuestions(), initialData.participants());
    sessionSseService.sendToEmitter(emitter, "LOBBY_STATE", sessionNumber, sessionId, lobbyPayload);

    if (initialData.endedEvent() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "SESSION_ENDED",
          sessionNumber,
          sessionId,
          toSessionEndedPayload(initialData.endedEvent()));
      return;
    }

    if (initialData.startEvent() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "QUESTION_START",
          sessionNumber,
          sessionId,
          toQuestionStartPayload(initialData.startEvent()));
    }

    if (initialData.pausedTimestamp() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "SESSION_PAUSE_STATUS",
          sessionNumber,
          sessionId,
          new SessionPausePayload(true, initialData.pausedTimestamp()));
    }

    if (initialData.revealEvent() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "QUESTION_REVEAL",
          sessionNumber,
          sessionId,
          toQuestionRevealPayload(initialData.revealEvent()));
    }

    if (initialData.leaderboardEntries() != null) {
      sessionSseService.sendToEmitter(
          emitter,
          "INTERMEDIATE_LEADERBOARD",
          sessionNumber,
          sessionId,
          toIntermediateLeaderboardPayload(
              initialData.leaderboardSlideIndex(), initialData.leaderboardEntries()));
    }
  }

  private static LobbyStatePayload toLobbyStatePayload(
      String sessionName, int totalQuestions, List<ParticipantView> participants) {
    final var participantDtos =
        participants.stream().map(p -> new ParticipantDto(p.name(), p.joinedAt())).toList();
    return new LobbyStatePayload(
        sessionName, participantDtos.size(), participantDtos, totalQuestions);
  }

  private static QuestionStartPayload toQuestionStartPayload(QuestionStartEvent event) {
    final var q = event.question();
    final var safeAnswers =
        q.answers().stream()
            .map(a -> new SafeAnswerDto(a.index(), a.label(), a.pictureUrl()))
            .toList();
    final var safeQuestion =
        new SafeQuestionDto(q.id(), q.type(), q.label(), q.pictureUrl(), safeAnswers);
    return new QuestionStartPayload(
        event.slideIndex(),
        event.totalSlides(),
        event.timerSeconds(),
        event.points(),
        safeQuestion,
        event.deadline(),
        event.answeredCount(),
        event.remainingSeconds());
  }

  private static QuestionRevealPayload toQuestionRevealPayload(QuestionRevealEvent event) {
    final var stats =
        event.answersStats().stream()
            .map(s -> new AnswerStatDto(s.answerIndex(), s.count(), s.percentage()))
            .toList();
    return new QuestionRevealPayload(
        event.slideIndex(),
        event.correctAnswerIndices(),
        event.explanation(),
        stats,
        event.mobileCorrectionEnabled(),
        event.acceptedAnswers());
  }

  private static IntermediateLeaderboardPayload toIntermediateLeaderboardPayload(
      int slideIndex, List<SessionLeaderboardEntry> entries) {
    final var dtos =
        entries.stream()
            .map(
                e ->
                    new LeaderboardEntryDto(
                        e.rank(), e.participantName(), e.score(), e.delta(), e.streak()))
            .toList();
    return new IntermediateLeaderboardPayload(slideIndex, dtos);
  }

  private static SessionEndedPayload toSessionEndedPayload(SessionEndedEvent event) {
    final var finalLeaderboard =
        event.finalLeaderboard().stream()
            .map(
                e ->
                    new FinalLeaderboardEntryDto(
                        e.rank(), e.participantName(), e.score(), e.correctCount()))
            .toList();
    final var top5 =
        event.top5().stream()
            .map(
                e ->
                    new FinalLeaderboardEntryDto(
                        e.rank(), e.participantName(), e.score(), e.correctCount()))
            .toList();
    final var podium = new PodiumDto(event.firstName(), event.secondName(), event.thirdName());
    final var metrics =
        new SessionGroupMetricsDto(
            event.metrics().totalParticipants(),
            event.metrics().averageScore(),
            event.metrics().globalSuccessRate());
    return new SessionEndedPayload(event.endedAt(), finalLeaderboard, podium, metrics, top5);
  }
}

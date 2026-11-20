package com.quezap.application.ports.sessions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

public interface SessionStorePort {

  record SessionSummary(
      UUID id,
      long number,
      String name,
      String status,
      int currentSlideIndex,
      int totalSlides,
      int activeParticipantCount,
      UUID authorId,
      @Nullable Instant startedAt,
      @Nullable Instant endedAt,
      String progressionMode,
      boolean isPaused,
      int explanationDurationSeconds,
      boolean mobileCorrectionEnabled) {}

  record SessionQuezapSnapshot(
      UUID id, String title, UUID authorId, boolean isPublic, List<SessionSlideSpec> slides) {}

  Optional<Session> findSessionById(UUID sessionId);

  Optional<Session> findSessionByNumber(long sessionNumber);

  boolean existsSessionByNumber(long sessionNumber);

  PageOf<SessionSummary> findSessionsByAuthorId(UUID authorId, Pagination pagination);

  Optional<SessionQuezapSnapshot> findQuezapById(UUID quezapId);

  Optional<UUID> findUserIdByPrincipal(String principal);

  Session saveSession(Session session);

  Session saveAndFlushSession(Session session);

  Optional<Session> findSessionHeaderAndSlidesById(UUID sessionId);

  Optional<SessionParticipant> findParticipantByToken(UUID sessionId, String participationToken);

  Optional<SessionAnswerRecord> findAnswerForParticipantAndSlide(
      UUID sessionId, String participantName, int slideIndex);

  void saveParticipantAndAnswer(
      UUID sessionId,
      SessionParticipant participant,
      SessionAnswerRecord answerRecord,
      boolean isNewAnswer);

  int countActiveParticipants(UUID sessionId);

  int countActiveAnswersForSlide(UUID sessionId, int slideIndex);
}

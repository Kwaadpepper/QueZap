package com.quezap.application.usecases.sessions;

import java.time.Instant;

import com.quezap.application.annotations.Usecase;
import com.quezap.application.ports.sessions.SessionRealtimePublisher;
import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.application.ports.sessions.SubmitSessionAnswer.Input;
import com.quezap.application.ports.sessions.SubmitSessionAnswer.Output;
import com.quezap.application.ports.sessions.SubmitSessionAnswer.SubmitSessionAnswerUsecase;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.utils.UuidV7;

@Usecase
public final class SubmitSessionAnswerHandler implements SubmitSessionAnswerUsecase {

  private final SessionStorePort sessionStore;
  private final SessionRealtimePublisher realtimePublisher;

  public SubmitSessionAnswerHandler(
      SessionStorePort sessionStore, SessionRealtimePublisher realtimePublisher) {
    this.sessionStore = sessionStore;
    this.realtimePublisher = realtimePublisher;
  }

  @Override
  public Output handle(Input input, UnitOfWorkEvents unitOfWork) {
    final var session =
        sessionStore
            .findSessionHeaderAndSlidesById(input.sessionId())
            .orElseThrow(() -> SessionDomainException.notFound("Session not found"));

    if (session.getStartedAt() == null || session.getEndedAt() != null) {
      throw SessionDomainException.badRequest("Session is not currently active");
    }
    if (session.isPaused()) {
      throw SessionDomainException.conflict("La session est actuellement en pause");
    }
    if (input.textAnswer() != null && input.textAnswer().length() > 255) {
      throw SessionDomainException.badRequest(
          "La réponse texte ne doit pas dépasser 255 caractères");
    }
    if (input.slideIndex() != null && !input.slideIndex().equals(session.getCurrentSlideIndex())) {
      throw SessionDomainException.badRequest("Invalid slide index for current question");
    }

    final var token = input.participationToken();
    if (token == null || token.isBlank()) {
      throw SessionDomainException.unauthorized("Invalid participant token");
    }

    final var participant =
        sessionStore
            .findParticipantByToken(input.sessionId(), token)
            .orElseThrow(() -> SessionDomainException.unauthorized("Invalid participant token"));

    if (participant.isKicked()) {
      throw SessionDomainException.forbidden("Participant was kicked from the session");
    }

    final var effectiveSlideIndex =
        input.slideIndex() != null
            ? input.slideIndex()
            : Math.max(session.getCurrentSlideIndex(), 1);

    final var now = Instant.now();
    final var evaluation =
        session.evaluateAnswer(input.slideIndex(), input.answerIndex(), input.textAnswer(), now);

    final var existingAnswerOpt =
        sessionStore.findAnswerForParticipantAndSlide(
            input.sessionId(), participant.name(), effectiveSlideIndex);

    final var effectiveAnswerIndex = input.answerIndex() != null ? input.answerIndex() : 0;
    final int previousPoints;
    final SessionAnswerRecord updatedAnswer;
    final boolean isNewAnswer;

    if (existingAnswerOpt.isPresent()) {
      final var existing = existingAnswerOpt.get();
      previousPoints = existing.pointsAwarded();
      updatedAnswer =
          new SessionAnswerRecord(
              existing.id(),
              participant.name(),
              effectiveSlideIndex,
              effectiveAnswerIndex,
              input.textAnswer(),
              evaluation.pointsAwarded(),
              evaluation.isCorrect(),
              now);
      isNewAnswer = false;
    } else {
      previousPoints = 0;
      updatedAnswer =
          new SessionAnswerRecord(
              UuidV7.randomUuid(),
              participant.name(),
              effectiveSlideIndex,
              effectiveAnswerIndex,
              input.textAnswer(),
              evaluation.pointsAwarded(),
              evaluation.isCorrect(),
              now);
      isNewAnswer = true;
    }

    final var newScore =
        Math.max(0, participant.score() - previousPoints + evaluation.pointsAwarded());
    final var updatedParticipant =
        participant.withUpdatedAnswerTimingAndScore(
            newScore, effectiveSlideIndex, evaluation.elapsedMillis());

    sessionStore.saveParticipantAndAnswer(
        input.sessionId(), updatedParticipant, updatedAnswer, isNewAnswer);

    final var totalActive = sessionStore.countActiveParticipants(input.sessionId());
    final var totalAnswered =
        sessionStore.countActiveAnswersForSlide(input.sessionId(), effectiveSlideIndex);
    final var sessionId = session.getId().value();
    final var sessionNum = session.getNumber().value();

    unitOfWork.onCommit(
        () ->
            realtimePublisher.publishAnswerCount(
                sessionId, sessionNum, effectiveSlideIndex, totalAnswered, totalActive));

    return new Output(true, now);
  }
}

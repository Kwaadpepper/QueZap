package com.quezap.infrastructure.adapter.persistence.jpa.mappers;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionName;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.participations.Participant;
import com.quezap.domain.models.valueobjects.participations.ParticipantName;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;
import com.quezap.domain.models.valueobjects.questions.QuestionAnswer;
import com.quezap.domain.models.valueobjects.questions.QuestionSlide;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionAnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

import org.jspecify.annotations.Nullable;

@Component
public class SessionJpaMapper {

  public SessionJpaEntity toEntity(
      Session domain,
      @Nullable UserJpaEntity authorEntity,
      Map<UUID, QuestionJpaEntity> questionEntities) {

    final var entity =
        new SessionJpaEntity(
            domain.getId().value(),
            domain.getNumber().value(),
            domain.getName().value(),
            domain.getCurrentSlideIndex(),
            domain.getAuthor().value(),
            null,
            domain.getStartedAt() != null ? domain.getStartedAt().position() : null,
            domain.getEndedAt() != null ? domain.getEndedAt().position() : null,
            domain.getCreatedAt().position());

    entity.setProgressionMode(domain.getProgressionMode().name());
    entity.setIsPaused(domain.isPaused());
    entity.setExplanationDurationSeconds(domain.getExplanationDurationSeconds());
    entity.setMobileCorrectionEnabled(domain.isMobileCorrectionEnabled());

    if (!domain.getQuestionSlides().isEmpty() && authorEntity != null) {
      final var quezap =
          new QuezapJpaEntity(
              UuidV7.randomUuid(),
              domain.getName().value(),
              null,
              authorEntity,
              false,
              Instant.now(),
              Instant.now());

      var order = 0;
      for (final var slide : domain.getQuestionSlides()) {
        final var questionEntity = questionEntities.get(slide.question().value());
        if (questionEntity != null) {
          final var qq =
              new QuezapQuestionJpaEntity(
                  UuidV7.randomUuid(),
                  quezap,
                  questionEntity,
                  slide.timer(),
                  slide.points(),
                  order++);
          quezap.addQuestion(qq);
        }
      }
      entity.setQuezap(quezap);
    }

    for (final var p : domain.getParticipants()) {
      final var sp =
          new SessionParticipantJpaEntity(
              UuidV7.randomUuid(),
              entity,
              p.name().value(),
              p.score(),
              p.token().value(),
              false,
              Instant.now());
      entity.addParticipant(sp);
    }

    for (final var qa : domain.getAnswers()) {
      final var sa =
          new SessionAnswerJpaEntity(
              UuidV7.randomUuid(),
              entity,
              qa.participant().value(),
              qa.slideIndex(),
              qa.answerIndex(),
              0,
              false,
              Instant.now());
      entity.addAnswer(sa);
    }

    return entity;
  }

  public Session toDomain(SessionJpaEntity entity) {
    final var questionSlides = new HashSet<QuestionSlide>();
    final var quezap = entity.getQuezap();
    if (quezap != null) {
      for (final var qq : quezap.getQuestions()) {
        questionSlides.add(
            new QuestionSlide(
                qq.getTimerSeconds(), qq.getPoints(), new QuestionId(qq.getQuestion().getId())));
      }
    }

    final var participants = new HashSet<Participant>();
    for (final var sp : entity.getParticipants()) {
      participants.add(
          new Participant(
              new ParticipantName(sp.getName()),
              sp.getScore(),
              new ParticipationToken(sp.getParticipationToken())));
    }

    final var answers = new HashSet<QuestionAnswer>();
    for (final var sa : entity.getAnswers()) {
      answers.add(
          new QuestionAnswer(
              new ParticipantName(sa.getParticipantName()),
              sa.getSlideIndex(),
              sa.getAnswerIndex()));
    }

    ProgressionMode progressionMode = ProgressionMode.MANUAL;
    if (entity.getProgressionMode() != null) {
      try {
        progressionMode = ProgressionMode.valueOf(entity.getProgressionMode());
      } catch (IllegalArgumentException ignored) {
        // Fallback to MANUAL
      }
    }

    return Session.hydrate(
        new SessionId(entity.getId()),
        new SessionName(entity.getName()),
        new SessionNumber(entity.getNumber()),
        entity.getCurrentSlideIndex(),
        questionSlides,
        participants,
        answers,
        new UserId(entity.getAuthorId()),
        progressionMode,
        Boolean.TRUE.equals(entity.getIsPaused()),
        entity.getExplanationDurationSeconds() != null
            ? entity.getExplanationDurationSeconds()
            : Session.DEFAULT_EXPLANATION_DURATION_SECONDS,
        Boolean.TRUE.equals(entity.getMobileCorrectionEnabled()),
        entity.getStartedAt() != null ? TimelinePoint.at(entity.getStartedAt()) : null,
        entity.getEndedAt() != null ? TimelinePoint.at(entity.getEndedAt()) : null);
  }
}

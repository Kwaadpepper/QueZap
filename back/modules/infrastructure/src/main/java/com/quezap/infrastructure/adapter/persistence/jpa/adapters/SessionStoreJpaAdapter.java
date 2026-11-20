package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.application.ports.sessions.SessionStorePort;
import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.ProgressionMode;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.models.valueobjects.sessions.SessionAnswerRecord;
import com.quezap.domain.models.valueobjects.sessions.SessionParticipant;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideOption;
import com.quezap.domain.models.valueobjects.sessions.SessionSlideSpec;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.AnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapQuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionAnswerJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionParticipantJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import jakarta.persistence.EntityManager;

@Repository
public class SessionStoreJpaAdapter implements SessionStorePort {

  private final SessionJpaRepository sessionJpaRepository;
  private final QuezapJpaRepository quezapJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final EntityManager entityManager;

  public SessionStoreJpaAdapter(
      SessionJpaRepository sessionJpaRepository,
      QuezapJpaRepository quezapJpaRepository,
      UserJpaRepository userJpaRepository,
      EntityManager entityManager) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.quezapJpaRepository = quezapJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> findSessionById(UUID sessionId) {
    return sessionJpaRepository.findById(sessionId).map(e -> toDomain(e, true));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> findSessionByNumber(long sessionNumber) {
    return sessionJpaRepository.findByNumber(sessionNumber).map(e -> toDomain(e, true));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsSessionByNumber(long sessionNumber) {
    return sessionJpaRepository.existsByNumber(sessionNumber);
  }

  @Override
  @Transactional(readOnly = true)
  public PageOf<SessionSummary> findSessionsByAuthorId(UUID authorId, Pagination pagination) {
    final var pageNumber = (int) Math.max(pagination.pageNumber() - 1, 0);
    final var pageSize = (int) pagination.pageSize();
    final var pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
    final var page = sessionJpaRepository.findByAuthorId(authorId, pageRequest);
    final var entities = page.getContent();

    final var participantCounts = new HashMap<UUID, Integer>();
    final var slideCounts = new HashMap<UUID, Integer>();
    if (!entities.isEmpty()) {
      final var sessionIds = entities.stream().map(SessionJpaEntity::getId).toList();
      for (final var row : sessionJpaRepository.countActiveParticipantsBySessionIds(sessionIds)) {
        participantCounts.put((UUID) row[0], ((Number) row[1]).intValue());
      }
      final var quezapIds =
          entities.stream()
              .map(SessionJpaEntity::getQuezap)
              .filter(Objects::nonNull)
              .map(QuezapJpaEntity::getId)
              .distinct()
              .toList();
      if (!quezapIds.isEmpty()) {
        for (final var row : sessionJpaRepository.countSlidesByQuezapIds(quezapIds)) {
          slideCounts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
      }
    }

    final var items =
        entities.stream()
            .map(
                s -> {
                  final var q = s.getQuezap();
                  final var totalSlides = q != null ? slideCounts.getOrDefault(q.getId(), 0) : 0;
                  final var activeCount = participantCounts.getOrDefault(s.getId(), 0);
                  return new SessionSummary(
                      s.getId(),
                      s.getNumber(),
                      s.getName(),
                      computeStatus(s),
                      s.getCurrentSlideIndex(),
                      totalSlides,
                      activeCount,
                      s.getAuthorId(),
                      s.getStartedAt(),
                      s.getEndedAt(),
                      s.getProgressionMode(),
                      Boolean.TRUE.equals(s.getIsPaused()),
                      s.getExplanationDurationSeconds(),
                      Boolean.TRUE.equals(s.getMobileCorrectionEnabled()));
                })
            .toList();

    return PageOf.of(pagination, items, page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SessionQuezapSnapshot> findQuezapById(UUID quezapId) {
    return quezapJpaRepository.findById(quezapId).map(this::toQuezapSnapshot);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> findUserIdByPrincipal(String principal) {
    try {
      final var userId = UUID.fromString(principal);
      return userJpaRepository.findById(userId).map(UserJpaEntity::getId);
    } catch (IllegalArgumentException _) {
      return userJpaRepository.findByName(principal).map(UserJpaEntity::getId);
    }
  }

  @Override
  @Transactional
  public Session saveSession(Session session) {
    final var entity = toManagedEntity(session);
    final var saved = sessionJpaRepository.save(entity);
    return toDomain(saved, true);
  }

  @Override
  @Transactional
  public Session saveAndFlushSession(Session session) {
    final var entity = toManagedEntity(session);
    final var saved = sessionJpaRepository.saveAndFlush(entity);
    return toDomain(saved, true);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> findSessionHeaderAndSlidesById(UUID sessionId) {
    return sessionJpaRepository.findByIdWithQuezap(sessionId).map(e -> toDomain(e, false));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SessionParticipant> findParticipantByToken(
      UUID sessionId, String participationToken) {
    return sessionJpaRepository
        .findParticipantBySessionIdAndToken(sessionId, participationToken)
        .map(SessionStoreJpaAdapter::toParticipantDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SessionAnswerRecord> findAnswerForParticipantAndSlide(
      UUID sessionId, String participantName, int slideIndex) {
    return sessionJpaRepository
        .findAnswerBySessionIdAndParticipantAndSlide(sessionId, participantName, slideIndex)
        .map(SessionStoreJpaAdapter::toAnswerDomain);
  }

  @Override
  @Transactional
  public void saveParticipantAndAnswer(
      UUID sessionId,
      SessionParticipant participant,
      SessionAnswerRecord answerRecord,
      boolean isNewAnswer) {
    final var participantEntity =
        Optional.ofNullable(entityManager.find(SessionParticipantJpaEntity.class, participant.id()))
            .orElseThrow(
                () -> new IllegalStateException("Participant not found: " + participant.id()));

    participantEntity.setScore(participant.score());
    participantEntity.setTotalResponseTimeMs(participant.totalResponseTimeMs());
    participantEntity.setLastAnsweredSlideIndex(participant.lastAnsweredSlideIndex());
    participantEntity.setLastSlideResponseTimeMs(participant.lastSlideResponseTimeMs());

    if (isNewAnswer) {
      final var sessionRef = sessionJpaRepository.getReferenceById(sessionId);
      final var answerEntity =
          new SessionAnswerJpaEntity(
              answerRecord.id(),
              sessionRef,
              answerRecord.participantName(),
              answerRecord.slideIndex(),
              answerRecord.answerIndex(),
              answerRecord.textAnswer(),
              answerRecord.pointsAwarded(),
              answerRecord.isCorrect(),
              answerRecord.answeredAt());
      entityManager.persist(answerEntity);
    } else {
      final var answerEntity =
          Optional.ofNullable(entityManager.find(SessionAnswerJpaEntity.class, answerRecord.id()))
              .orElseThrow(
                  () -> new IllegalStateException("Answer not found: " + answerRecord.id()));
      answerEntity.setAnswerIndex(answerRecord.answerIndex());
      answerEntity.setTextAnswer(answerRecord.textAnswer());
      answerEntity.setPointsAwarded(answerRecord.pointsAwarded());
      answerEntity.setIsCorrect(answerRecord.isCorrect());
      answerEntity.setAnsweredAt(answerRecord.answeredAt());
    }
    entityManager.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public int countActiveParticipants(UUID sessionId) {
    return (int) sessionJpaRepository.countActiveParticipants(sessionId);
  }

  @Override
  @Transactional(readOnly = true)
  public int countActiveAnswersForSlide(UUID sessionId, int slideIndex) {
    return (int) sessionJpaRepository.countActiveAnswersForSlide(sessionId, slideIndex);
  }

  private SessionQuezapSnapshot toQuezapSnapshot(QuezapJpaEntity quezap) {
    return new SessionQuezapSnapshot(
        quezap.getId(),
        quezap.getTitle(),
        quezap.getAuthor().getId(),
        Boolean.TRUE.equals(quezap.getIsPublic()),
        toSlideSpecs(quezap));
  }

  private SessionJpaEntity toManagedEntity(Session domain) {
    final var existingOpt = sessionJpaRepository.findById(domain.getId().value());
    if (existingOpt.isPresent()) {
      final var entity = existingOpt.get();
      applyScalarFields(entity, domain);
      syncParticipants(entity, domain.getSessionParticipants());
      syncAnswers(entity, domain.getSessionAnswers());
      return entity;
    }

    final var quezapEntity =
        domain.getQuezapId() != null
            ? quezapJpaRepository.findById(domain.getQuezapId()).orElse(null)
            : null;
    final var entity =
        new SessionJpaEntity(
            domain.getId().value(),
            domain.getNumber().value(),
            domain.getRawName(),
            domain.getCurrentSlideIndex(),
            domain.getAuthor().value(),
            quezapEntity,
            domain.getStartedAt() != null ? domain.getStartedAt().position() : null,
            domain.getEndedAt() != null ? domain.getEndedAt().position() : null,
            domain.getCreatedAt().position());
    if (domain.getQuezapTitle() != null) {
      entity.setQuezapTitle(domain.getQuezapTitle());
    }
    applyScalarFields(entity, domain);
    syncParticipants(entity, domain.getSessionParticipants());
    syncAnswers(entity, domain.getSessionAnswers());
    return entity;
  }

  private static void applyScalarFields(SessionJpaEntity entity, Session domain) {
    entity.setName(domain.getRawName());
    entity.setCurrentSlideIndex(domain.getCurrentSlideIndex());
    entity.setStartedAt(domain.getStartedAt() != null ? domain.getStartedAt().position() : null);
    entity.setCurrentSlideStartedAt(
        domain.getCurrentSlideStartedAt() != null
            ? domain.getCurrentSlideStartedAt().position()
            : null);
    entity.setPausedAt(domain.getPausedAt() != null ? domain.getPausedAt().position() : null);
    entity.setEndedAt(domain.getEndedAt() != null ? domain.getEndedAt().position() : null);
    entity.setLastRevealedSlideIndex(domain.getLastRevealedSlideIndex());
    entity.setProgressionMode(domain.getProgressionMode().name());
    entity.setIsPaused(domain.isPaused());
    entity.setExplanationDurationSeconds(domain.getExplanationDurationSeconds());
    entity.setMobileCorrectionEnabled(domain.isMobileCorrectionEnabled());
  }

  private static void syncParticipants(
      SessionJpaEntity entity, List<SessionParticipant> domainParticipants) {
    final var domainById = new HashMap<UUID, SessionParticipant>();
    for (final var p : domainParticipants) {
      domainById.put(p.id(), p);
    }

    entity.getParticipants().removeIf(p -> !domainById.containsKey(p.getId()));

    final var existingById = new HashMap<UUID, SessionParticipantJpaEntity>();
    for (final var p : entity.getParticipants()) {
      existingById.put(p.getId(), p);
    }

    for (final var p : domainParticipants) {
      final var existing = existingById.get(p.id());
      if (existing != null) {
        existing.setScore(p.score());
        existing.setIsKicked(p.isKicked());
        existing.setTotalResponseTimeMs(p.totalResponseTimeMs());
        existing.setLastAnsweredSlideIndex(p.lastAnsweredSlideIndex());
        existing.setLastSlideResponseTimeMs(p.lastSlideResponseTimeMs());
      } else {
        final var created =
            new SessionParticipantJpaEntity(
                p.id(),
                entity,
                p.name(),
                p.score(),
                p.participationToken(),
                p.isKicked(),
                p.joinedAt());
        created.setTotalResponseTimeMs(p.totalResponseTimeMs());
        created.setLastAnsweredSlideIndex(p.lastAnsweredSlideIndex());
        created.setLastSlideResponseTimeMs(p.lastSlideResponseTimeMs());
        entity.addParticipant(created);
      }
    }
  }

  private static void syncAnswers(
      SessionJpaEntity entity, List<SessionAnswerRecord> domainAnswers) {
    final var domainIds = new HashSet<UUID>();
    for (final var a : domainAnswers) {
      domainIds.add(a.id());
    }
    entity.getAnswers().removeIf(a -> !domainIds.contains(a.getId()));

    final var existingById = new HashMap<UUID, SessionAnswerJpaEntity>();
    for (final var a : entity.getAnswers()) {
      existingById.put(a.getId(), a);
    }

    for (final var a : domainAnswers) {
      final var existing = existingById.get(a.id());
      if (existing != null) {
        existing.setAnswerIndex(a.answerIndex());
        existing.setTextAnswer(a.textAnswer());
        existing.setPointsAwarded(a.pointsAwarded());
        existing.setIsCorrect(a.isCorrect());
        existing.setAnsweredAt(a.answeredAt());
      } else {
        entity.addAnswer(
            new SessionAnswerJpaEntity(
                a.id(),
                entity,
                a.participantName(),
                a.slideIndex(),
                a.answerIndex(),
                a.textAnswer(),
                a.pointsAwarded(),
                a.isCorrect(),
                a.answeredAt()));
      }
    }
  }

  private Session toDomain(SessionJpaEntity entity, boolean includeCollections) {
    final var quezap = entity.getQuezap();
    final var slides = quezap != null ? toSlideSpecs(quezap) : List.<SessionSlideSpec>of();
    final var participants =
        includeCollections
            ? entity.getParticipants().stream()
                .map(SessionStoreJpaAdapter::toParticipantDomain)
                .toList()
            : List.<SessionParticipant>of();
    final var answers =
        includeCollections
            ? entity.getAnswers().stream().map(SessionStoreJpaAdapter::toAnswerDomain).toList()
            : List.<SessionAnswerRecord>of();

    ProgressionMode progressionMode = ProgressionMode.MANUAL;
    if (entity.getProgressionMode() != null) {
      try {
        progressionMode =
            ProgressionMode.valueOf(entity.getProgressionMode().trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        // Fallback to MANUAL
      }
    }

    return Session.hydrateLive(
        new SessionId(entity.getId()),
        entity.getName(),
        new SessionNumber(entity.getNumber()),
        entity.getCurrentSlideIndex(),
        new UserId(entity.getAuthorId()),
        quezap != null ? quezap.getId() : null,
        quezap != null ? quezap.getTitle() : entity.getQuezapTitle(),
        progressionMode,
        Boolean.TRUE.equals(entity.getIsPaused()),
        entity.getExplanationDurationSeconds() != null
            ? entity.getExplanationDurationSeconds()
            : Session.DEFAULT_EXPLANATION_DURATION_SECONDS,
        Boolean.TRUE.equals(entity.getMobileCorrectionEnabled()),
        entity.getStartedAt() != null ? TimelinePoint.at(entity.getStartedAt()) : null,
        entity.getCurrentSlideStartedAt() != null
            ? TimelinePoint.at(entity.getCurrentSlideStartedAt())
            : null,
        entity.getPausedAt() != null ? TimelinePoint.at(entity.getPausedAt()) : null,
        entity.getEndedAt() != null ? TimelinePoint.at(entity.getEndedAt()) : null,
        entity.getLastRevealedSlideIndex(),
        TimelinePoint.at(entity.getCreatedAt()),
        slides,
        participants,
        answers);
  }

  private List<SessionSlideSpec> toSlideSpecs(QuezapJpaEntity quezap) {
    final var sortedQuestions =
        quezap.getQuestions().stream()
            .sorted(Comparator.comparing(QuezapQuestionJpaEntity::getQuestionOrder))
            .toList();
    final var specs = new ArrayList<SessionSlideSpec>(sortedQuestions.size());
    for (final var qq : sortedQuestions) {
      final var q = qq.getQuestion();
      final var sortedAnswers =
          q.getAnswers().stream()
              .sorted(Comparator.comparing(AnswerJpaEntity::getAnswerOrder))
              .toList();
      final var options = new ArrayList<SessionSlideOption>(sortedAnswers.size());
      for (var i = 0; i < sortedAnswers.size(); i++) {
        final var a = sortedAnswers.get(i);
        options.add(
            new SessionSlideOption(
                i, a.getLabel(), a.getPictureFilename(), Boolean.TRUE.equals(a.getIsCorrect())));
      }
      specs.add(
          new SessionSlideSpec(
              qq.getQuestionOrder(),
              q.getId(),
              q.getType(),
              q.getLabel(),
              q.getPictureFilename(),
              qq.getTimerSeconds(),
              qq.getPoints(),
              options));
    }
    return specs;
  }

  private static SessionParticipant toParticipantDomain(SessionParticipantJpaEntity p) {
    return new SessionParticipant(
        p.getId(),
        p.getName(),
        p.getScore(),
        p.getParticipationToken(),
        Boolean.TRUE.equals(p.getIsKicked()),
        p.getJoinedAt(),
        p.getTotalResponseTimeMs(),
        p.getLastAnsweredSlideIndex(),
        p.getLastSlideResponseTimeMs());
  }

  private static SessionAnswerRecord toAnswerDomain(SessionAnswerJpaEntity a) {
    return new SessionAnswerRecord(
        a.getId(),
        a.getParticipantName(),
        a.getSlideIndex(),
        a.getAnswerIndex(),
        a.getTextAnswer(),
        a.getPointsAwarded(),
        Boolean.TRUE.equals(a.getIsCorrect()),
        a.getAnsweredAt());
  }

  private static String computeStatus(SessionJpaEntity s) {
    if (s.getEndedAt() != null) {
      return "ENDED";
    }
    if (s.getStartedAt() != null) {
      return "STARTED";
    }
    return "LOBBY";
  }
}

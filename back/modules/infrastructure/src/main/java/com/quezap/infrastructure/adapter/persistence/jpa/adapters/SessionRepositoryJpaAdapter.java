package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.ports.repositories.SessionRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.SessionJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuestionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

@Repository
@Primary
public class SessionRepositoryJpaAdapter implements SessionRepository {

  private final SessionJpaRepository sessionJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final QuestionJpaRepository questionJpaRepository;
  private final SessionJpaMapper sessionJpaMapper;

  public SessionRepositoryJpaAdapter(
      SessionJpaRepository sessionJpaRepository,
      UserJpaRepository userJpaRepository,
      QuestionJpaRepository questionJpaRepository,
      SessionJpaMapper sessionJpaMapper) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.sessionJpaMapper = sessionJpaMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> find(SessionId id) {
    if (id == null || id.value() == null) {
      return Optional.empty();
    }
    return sessionJpaRepository.findById(id.value()).map(sessionJpaMapper::toDomain);
  }

  @Override
  @Transactional
  public void persist(Session entity) {
    final var author = userJpaRepository.findById(entity.getAuthor().value()).orElse(null);

    final var questionMap = new HashMap<UUID, QuestionJpaEntity>();
    for (final var slide : entity.getQuestionSlides()) {
      questionJpaRepository
          .findById(slide.question().value())
          .ifPresent(q -> questionMap.put(slide.question().value(), q));
    }

    final var jpaEntity = sessionJpaMapper.toEntity(entity, author, questionMap);
    sessionJpaRepository.save(jpaEntity);
  }

  @Override
  @Transactional
  public void delete(Session entity) {
    sessionJpaRepository.deleteById(entity.getId().value());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> findByNumber(SessionNumber number) {
    return sessionJpaRepository.findByNumber(number.value()).map(sessionJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Session> latestByNumber() {
    return sessionJpaRepository.findFirstByOrderByNumberDesc().map(sessionJpaMapper::toDomain);
  }
}

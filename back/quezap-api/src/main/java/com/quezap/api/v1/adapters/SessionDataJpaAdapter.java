package com.quezap.api.v1.adapters;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.quezap.api.v1.ports.SessionDataPort;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.QuezapJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.SessionJpaRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.UserJpaRepository;

@Component
public class SessionDataJpaAdapter implements SessionDataPort {

  private final SessionJpaRepository sessionJpaRepository;
  private final QuezapJpaRepository quezapJpaRepository;
  private final UserJpaRepository userJpaRepository;

  public SessionDataJpaAdapter(
      SessionJpaRepository sessionJpaRepository,
      QuezapJpaRepository quezapJpaRepository,
      UserJpaRepository userJpaRepository) {
    this.sessionJpaRepository = sessionJpaRepository;
    this.quezapJpaRepository = quezapJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public Optional<SessionJpaEntity> findSessionById(UUID id) {
    return sessionJpaRepository.findById(id);
  }

  @Override
  public Optional<SessionJpaEntity> findSessionByNumber(Long number) {
    return sessionJpaRepository.findByNumber(number);
  }

  @Override
  public boolean existsSessionByNumber(Long number) {
    return sessionJpaRepository.existsByNumber(number);
  }

  @Override
  public Page<SessionJpaEntity> findSessionsByAuthorId(UUID authorId, Pageable pageable) {
    return sessionJpaRepository.findByAuthorId(authorId, pageable);
  }

  @Override
  public Page<SessionJpaEntity> findAllSessions(Pageable pageable) {
    return sessionJpaRepository.findAll(pageable);
  }

  @Override
  public SessionJpaEntity saveSession(SessionJpaEntity session) {
    return sessionJpaRepository.save(session);
  }

  @Override
  public SessionJpaEntity saveAndFlushSession(SessionJpaEntity session) {
    return sessionJpaRepository.saveAndFlush(session);
  }

  @Override
  public void deleteSession(SessionJpaEntity session) {
    sessionJpaRepository.delete(session);
  }

  @Override
  public Optional<QuezapJpaEntity> findQuezapById(UUID quezapId) {
    return quezapJpaRepository.findById(quezapId);
  }

  @Override
  public Optional<UserJpaEntity> findUserById(UUID userId) {
    return userJpaRepository.findById(userId);
  }

  @Override
  public Optional<UserJpaEntity> findUserByName(String name) {
    return userJpaRepository.findByName(name);
  }
}

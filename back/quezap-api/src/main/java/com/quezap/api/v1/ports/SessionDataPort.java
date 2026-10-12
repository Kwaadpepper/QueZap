package com.quezap.api.v1.ports;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;

public interface SessionDataPort {

  Optional<SessionJpaEntity> findSessionById(UUID id);

  Optional<SessionJpaEntity> findSessionByNumber(Long number);

  boolean existsSessionByNumber(Long number);

  Page<SessionJpaEntity> findSessionsByAuthorId(UUID authorId, Pageable pageable);

  Page<SessionJpaEntity> findAllSessions(Pageable pageable);

  SessionJpaEntity saveSession(SessionJpaEntity session);

  SessionJpaEntity saveAndFlushSession(SessionJpaEntity session);

  void deleteSession(SessionJpaEntity session);

  Optional<QuezapJpaEntity> findQuezapById(UUID quezapId);

  Optional<UserJpaEntity> findUserById(UUID userId);

  Optional<UserJpaEntity> findUserByName(String name);
}

package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

  Page<SessionJpaEntity> findByAuthorId(UUID authorId, Pageable pageable);

  Optional<SessionJpaEntity> findByNumber(Long number);

  boolean existsByNumber(Long number);

  Optional<SessionJpaEntity> findFirstByOrderByNumberDesc();
}

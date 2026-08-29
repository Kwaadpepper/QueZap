package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.SessionJpaEntity;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

  Optional<SessionJpaEntity> findByNumber(Long number);

  Optional<SessionJpaEntity> findFirstByOrderByNumberDesc();
}

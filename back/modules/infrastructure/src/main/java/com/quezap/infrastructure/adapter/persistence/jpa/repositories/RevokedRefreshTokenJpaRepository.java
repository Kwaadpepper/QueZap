package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.RevokedRefreshTokenJpaEntity;

@Repository
public interface RevokedRefreshTokenJpaRepository
    extends JpaRepository<RevokedRefreshTokenJpaEntity, String> {

  @Modifying
  @Transactional
  @Query("DELETE FROM RevokedRefreshTokenJpaEntity r WHERE r.expiresAt < :now")
  void deleteByExpiresAtBefore(@Param("now") Instant now);
}

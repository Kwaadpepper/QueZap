package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuezapJpaEntity;

@Repository
public interface QuezapJpaRepository extends JpaRepository<QuezapJpaEntity, UUID> {

  Page<QuezapJpaEntity> findByAuthorId(UUID authorId, Pageable pageable);

  Page<QuezapJpaEntity> findByAuthorIdAndIsPublicTrue(UUID authorId, Pageable pageable);

  Page<QuezapJpaEntity> findByIsPublicTrue(Pageable pageable);

  Page<QuezapJpaEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);

  Page<QuezapJpaEntity> findByTitleContainingIgnoreCaseAndIsPublicTrue(
      String title, Pageable pageable);

  Page<QuezapJpaEntity> findByTitleContainingIgnoreCaseAndAuthorId(
      String title, UUID authorId, Pageable pageable);
}

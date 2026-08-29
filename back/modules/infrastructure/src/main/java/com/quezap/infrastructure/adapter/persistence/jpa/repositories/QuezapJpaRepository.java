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

  Page<QuezapJpaEntity> findByIsPublicTrue(Pageable pageable);

  Page<QuezapJpaEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}

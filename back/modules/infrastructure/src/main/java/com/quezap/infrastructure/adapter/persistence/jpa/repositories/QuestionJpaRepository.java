package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.QuestionJpaEntity;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

  long countByThemeIdIn(Collection<UUID> themeIds);

  Page<QuestionJpaEntity> findByThemeIdIn(Collection<UUID> themeIds, Pageable pageable);

  Page<QuestionJpaEntity> findByLabelContainingIgnoreCase(String label, Pageable pageable);

  Page<QuestionJpaEntity> findByThemeIdInAndLabelContainingIgnoreCase(
      Collection<UUID> themeIds, String label, Pageable pageable);
}

package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;

@Repository
public interface ThemeJpaRepository extends JpaRepository<ThemeJpaEntity, UUID> {

  Optional<ThemeJpaEntity> findByName(String name);

  Page<ThemeJpaEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

  Optional<UserJpaEntity> findByName(String name);

  Optional<UserJpaEntity> findByCredentialId(UUID credentialId);
}

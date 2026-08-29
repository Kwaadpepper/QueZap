package com.quezap.infrastructure.adapter.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;

@Repository
public interface CredentialJpaRepository extends JpaRepository<CredentialJpaEntity, UUID> {

  Optional<CredentialJpaEntity> findByHashedIdentifier(String hashedIdentifier);
}

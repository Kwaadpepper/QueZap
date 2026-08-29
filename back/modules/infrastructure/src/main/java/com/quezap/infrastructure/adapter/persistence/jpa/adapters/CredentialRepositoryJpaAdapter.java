package com.quezap.infrastructure.adapter.persistence.jpa.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.ports.repositories.CredentialRepository;
import com.quezap.infrastructure.adapter.persistence.jpa.mappers.CredentialJpaMapper;
import com.quezap.infrastructure.adapter.persistence.jpa.repositories.CredentialJpaRepository;

@Repository
@Primary
public class CredentialRepositoryJpaAdapter implements CredentialRepository {

  private final CredentialJpaRepository credentialJpaRepository;
  private final CredentialJpaMapper credentialJpaMapper;

  public CredentialRepositoryJpaAdapter(
      CredentialJpaRepository credentialJpaRepository, CredentialJpaMapper credentialJpaMapper) {
    this.credentialJpaRepository = credentialJpaRepository;
    this.credentialJpaMapper = credentialJpaMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Credential> find(CredentialId id) {
    if (id == null || id.value() == null) {
      return Optional.empty();
    }
    return credentialJpaRepository.findById(id.value()).map(credentialJpaMapper::toDomain);
  }

  @Override
  @Transactional
  public void persist(Credential entity) {
    final var jpaEntity = credentialJpaMapper.toEntity(entity);
    credentialJpaRepository.save(jpaEntity);
  }

  @Override
  @Transactional
  public void delete(Credential entity) {
    credentialJpaRepository.deleteById(entity.getId().value());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Credential> findByIdentifier(HashedIdentifier identifier) {
    if (identifier == null || identifier.value() == null) {
      return Optional.empty();
    }
    return credentialJpaRepository
        .findByHashedIdentifier(identifier.value())
        .map(credentialJpaMapper::toDomain);
  }
}

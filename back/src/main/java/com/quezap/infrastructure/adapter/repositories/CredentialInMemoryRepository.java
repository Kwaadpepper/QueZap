package com.quezap.infrastructure.adapter.repositories;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.port.repositories.CredentialRepository;

@Repository
public class CredentialInMemoryRepository implements CredentialRepository {
  private final ConcurrentHashMap<CredentialId, Credential> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<Credential> find(CredentialId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void persist(Credential entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(Credential entity) {
    storage.remove(entity.getId());
  }

  @Override
  public Optional<Credential> findByIdentifier(HashedIdentifier identifier) {
    return storage.values().stream()
        .filter(credential -> credential.getHashedIdentifier().equals(identifier))
        .findFirst();
  }
}

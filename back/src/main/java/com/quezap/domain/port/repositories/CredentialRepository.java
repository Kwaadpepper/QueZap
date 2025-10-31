package com.quezap.domain.port.repositories;

import java.util.Optional;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.lib.ddd.Repository;

import org.jspecify.annotations.NonNull;

public interface CredentialRepository extends Repository<Credential, @NonNull CredentialId> {
  public Optional<Credential> findByIdentifier(HashedIdentifier identifier);
}

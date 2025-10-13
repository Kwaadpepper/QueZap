package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.lib.ddd.Repository;

import org.eclipse.jdt.annotation.Nullable;

public interface CredentialRepository extends Repository<Credential> {
  public @Nullable Credential findByidentifier(HashedIdentifier identifier);
}

package com.quezap.domain.ports.repositories;

import java.util.Optional;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.repositories.Repository;

public interface UserRepository extends Repository<User, UserId> {
  public Optional<User> findByName(String name);

  public Optional<User> findByCredential(CredentialId credentialId);
}

package com.quezap.infrastructure.adapter.persistence.jpa.mappers;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.Credential;
import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Component
public class CredentialJpaMapper {

  public CredentialJpaEntity toEntity(Credential domain) {
    return new CredentialJpaEntity(
        domain.getId().value(),
        domain.getHashedIdentifier().value(),
        domain.getHashedPassword().value(),
        domain.getLastConnectionAt() != null ? domain.getLastConnectionAt().position() : null,
        domain.getCreatedAt().position(),
        domain.getUpdatedAt().position());
  }

  public Credential toDomain(CredentialJpaEntity entity) {
    return Credential.hydrate(
        new CredentialId(entity.getId()),
        new HashedPassword(entity.getHashedPassword()),
        new HashedIdentifier(entity.getHashedIdentifier()),
        entity.getLastConnectionAt() != null
            ? TimelinePoint.at(entity.getLastConnectionAt())
            : null,
        TimelinePoint.at(entity.getUpdatedAt()));
  }
}

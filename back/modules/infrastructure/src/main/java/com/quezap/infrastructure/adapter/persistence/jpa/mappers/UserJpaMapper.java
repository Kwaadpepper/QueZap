package com.quezap.infrastructure.adapter.persistence.jpa.mappers;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.CredentialId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.directories.views.UserView;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.CredentialJpaEntity;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.UserJpaEntity;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Component
public class UserJpaMapper {

  public UserJpaEntity toEntity(User domain, CredentialJpaEntity credentialEntity) {
    return new UserJpaEntity(
        domain.getId().value(),
        domain.getName(),
        credentialEntity,
        domain.getCreatedAt().position(),
        domain.getUpdatedAt().position());
  }

  public User toDomain(UserJpaEntity entity) {
    return User.hydrate(
        new UserId(entity.getId()),
        entity.getName(),
        new CredentialId(entity.getCredential().getId()),
        TimelinePoint.at(entity.getUpdatedAt()));
  }

  public UserView toView(UserJpaEntity entity) {
    return new UserView(
        new UserId(entity.getId()),
        entity.getName(),
        TimelinePoint.at(entity.getCreatedAt()),
        TimelinePoint.at(entity.getUpdatedAt()));
  }
}

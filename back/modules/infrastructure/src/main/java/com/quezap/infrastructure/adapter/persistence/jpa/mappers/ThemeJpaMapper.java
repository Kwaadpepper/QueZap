package com.quezap.infrastructure.adapter.persistence.jpa.mappers;

import org.springframework.stereotype.Component;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.directories.views.ThemeView;
import com.quezap.infrastructure.adapter.persistence.jpa.entities.ThemeJpaEntity;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

@Component
public class ThemeJpaMapper {

  public ThemeJpaEntity toEntity(Theme domain) {
    return new ThemeJpaEntity(
        domain.getId().value(), domain.getName().value(), domain.getCreatedAt().position());
  }

  public Theme toDomain(ThemeJpaEntity entity) {
    return Theme.hydrate(new ThemeId(entity.getId()), new ThemeName(entity.getName()));
  }

  public ThemeView toView(ThemeJpaEntity entity) {
    return new ThemeView(
        new ThemeId(entity.getId()),
        new ThemeName(entity.getName()),
        TimelinePoint.at(entity.getCreatedAt()));
  }
}

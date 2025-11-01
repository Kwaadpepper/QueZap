package com.quezap.domain.port.repositories;

import java.util.Optional;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.repositories.Repository;

public interface ThemeRepository extends Repository<Theme, ThemeId> {
  Optional<Theme> findByName(ThemeName name);
}

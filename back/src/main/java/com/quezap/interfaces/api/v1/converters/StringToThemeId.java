package com.quezap.interfaces.api.v1.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import org.jspecify.annotations.Nullable;

@Component
public class StringToThemeId implements Converter<String, ThemeId> {

  @Override
  public @Nullable ThemeId convert(@Nullable String source) {
    if (source == null || source.isBlank()) {
      return null;
    }

    return ThemeId.fromString(source);
  }
}

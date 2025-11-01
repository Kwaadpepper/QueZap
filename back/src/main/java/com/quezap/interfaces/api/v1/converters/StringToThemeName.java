package com.quezap.interfaces.api.v1.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.ThemeName;

import org.jspecify.annotations.Nullable;

@Component
public class StringToThemeName implements Converter<String, ThemeName> {

  @Override
  public @Nullable ThemeName convert(@Nullable String source) {
    if (source == null || source.isBlank()) {
      return null;
    }

    return new ThemeName(source);
  }
}
